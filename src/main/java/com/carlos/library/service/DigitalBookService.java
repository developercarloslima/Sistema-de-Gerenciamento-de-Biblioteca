package com.carlos.library.service;

import com.carlos.library.config.StorageProperties;
import com.carlos.library.domain.entity.*;
import com.carlos.library.domain.enums.*;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DigitalBookService {
    private static final Set<String> COVER_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final BookRepository books;
    private final BookAssetRepository assets;
    private final BookUploadSessionRepository sessions;
    private final UserAccountRepository users;
    private final DigitalStorageAvailability storageAvailability;
    private final StorageProperties properties;
    private final MapperService mapper;

    @Transactional
    public AssetUploadResponse createUpload(UUID bookId, AssetUploadRequest request, String email) {
        R2StorageService storage = storageAvailability.required();
        Book book = findBook(bookId);
        UserAccount user = findUser(email);
        validateRequest(request);

        sessions.findByBookIdAndAssetTypeAndStatus(bookId, request.assetType(), UploadStatus.PENDING)
                .forEach(session -> {
                    session.setStatus(UploadStatus.CANCELLED);
                    storage.deleteQuietly(session.getObjectKey());
                });

        String safeName = sanitizeFilename(request.filename());
        String extension = extensionFor(request.assetType(), request.contentType(), safeName);
        String objectKey = "books/%s/%s/%s.%s".formatted(
                bookId, request.assetType().name().toLowerCase(Locale.ROOT), UUID.randomUUID(), extension
        );
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusMinutes(properties.uploadExpirationMinutes());

        BookUploadSession session = new BookUploadSession();
        session.setBook(book);
        session.setAssetType(request.assetType());
        session.setObjectKey(objectKey);
        session.setOriginalFilename(safeName);
        session.setContentType(request.contentType().toLowerCase(Locale.ROOT));
        session.setExpectedSize(request.fileSize());
        session.setAccessLevel(request.accessLevel());
        session.setDownloadAllowed(request.assetType() == BookAssetType.COVER || request.downloadAllowed());
        session.setRequestedBy(user);
        session.setExpiresAt(expiresAt);
        session.setStatus(UploadStatus.PENDING);
        sessions.save(session);

        R2StorageService.PresignedUpload upload = storage.createUploadUrl(objectKey, session.getContentType());
        return new AssetUploadResponse(session.getId(), request.assetType(), upload.url(), "PUT",
                upload.headers(), expiresAt, properties.maxPdfBytes(), properties.maxCoverBytes());
    }

    @Transactional
    public BookAssetResponse confirmUpload(UUID bookId, UUID sessionId, String email) {
        R2StorageService storage = storageAvailability.required();
        BookUploadSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sessão de upload não encontrada."));
        if (!session.getBook().getId().equals(bookId)) throw new BusinessException("Sessão de upload inválida.");
        if (session.getStatus() != UploadStatus.PENDING) throw new BusinessException("Esta sessão de upload não está pendente.");
        if (!session.getRequestedBy().getEmail().equalsIgnoreCase(email)) throw new AccessDeniedException("Acesso negado.");
        if (session.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            session.setStatus(UploadStatus.EXPIRED);
            throw new BusinessException("A sessão de upload expirou. Gere uma nova URL.");
        }

        HeadObjectResponse head = storage.head(session.getObjectKey());
        if (head.contentLength() != session.getExpectedSize()) {
            storage.deleteQuietly(session.getObjectKey());
            session.setStatus(UploadStatus.CANCELLED);
            throw new BusinessException("O tamanho do arquivo recebido é diferente do informado.");
        }
        if (head.contentType() != null && !head.contentType().equalsIgnoreCase(session.getContentType())) {
            storage.deleteQuietly(session.getObjectKey());
            session.setStatus(UploadStatus.CANCELLED);
            throw new BusinessException("O tipo do arquivo recebido é diferente do informado.");
        }
        validateSignature(session, storage.readPrefix(session.getObjectKey(), 15));

        UserAccount user = findUser(email);
        BookAsset asset = assets.findByBookIdAndAssetType(bookId, session.getAssetType()).orElseGet(BookAsset::new);
        String oldKey = asset.getObjectKey();
        asset.setBook(session.getBook());
        asset.setAssetType(session.getAssetType());
        asset.setObjectKey(session.getObjectKey());
        asset.setOriginalFilename(session.getOriginalFilename());
        asset.setContentType(session.getContentType());
        asset.setFileSize(head.contentLength());
        asset.setEtag(stripQuotes(head.eTag()));
        asset.setAccessLevel(session.getAccessLevel());
        asset.setDownloadAllowed(session.isDownloadAllowed());
        asset.setUploadedBy(user);
        assets.save(asset);

        session.setStatus(UploadStatus.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (oldKey != null && !oldKey.equals(asset.getObjectKey())) storage.deleteQuietly(oldKey);
        return toResponse(asset);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> catalog(String query, Pageable pageable, String email) {
        Role role = findUser(email).getRole();
        Collection<DigitalAccessLevel> levels = switch (role) {
            case ADMIN -> List.of(DigitalAccessLevel.values());
            case LIBRARIAN -> List.of(DigitalAccessLevel.PUBLIC, DigitalAccessLevel.MEMBERS_ONLY, DigitalAccessLevel.STAFF_ONLY);
            case MEMBER -> List.of(DigitalAccessLevel.PUBLIC, DigitalAccessLevel.MEMBERS_ONLY);
        };
        String normalized = query == null ? "" : query.trim();
        return books.searchDigitalCatalog(normalized, levels, pageable).map(mapper::book);
    }

    @Transactional(readOnly = true)
    public List<BookAssetResponse> list(UUID bookId, String email) {
        findBook(bookId);
        Role role = findUser(email).getRole();
        return assets.findByBookIdOrderByAssetType(bookId).stream()
                .filter(asset -> canAccess(asset.getAccessLevel(), role))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetAccessResponse access(UUID bookId, BookAssetType type, boolean download, String email) {
        R2StorageService storage = storageAvailability.required();
        BookAsset asset = findAsset(bookId, type);
        UserAccount user = findUser(email);
        if (!canAccess(asset.getAccessLevel(), user.getRole())) throw new AccessDeniedException("Acesso negado.");
        if (download && !asset.isDownloadAllowed()) throw new BusinessException("O download deste livro não está autorizado.");
        R2StorageService.PresignedAccess access = storage.createAccessUrl(asset.getObjectKey());
        return new AssetAccessResponse(asset.getId(), asset.getAssetType(), access.url(),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(access.expiresInMinutes()),
                asset.getContentType(), asset.getOriginalFilename(), asset.isDownloadAllowed());
    }

    @Transactional
    public void delete(UUID bookId, BookAssetType type) {
        R2StorageService storage = storageAvailability.required();
        BookAsset asset = findAsset(bookId, type);
        assets.delete(asset);
        storage.deleteQuietly(asset.getObjectKey());
    }

    @Transactional
    public void expirePendingUploads() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        R2StorageService storage = storageAvailability.optional();
        sessions.findByStatusAndExpiresAtBefore(UploadStatus.PENDING, now).forEach(session -> {
            session.setStatus(UploadStatus.EXPIRED);
            if (storage != null) storage.deleteQuietly(session.getObjectKey());
        });
    }

    @Transactional(readOnly = true)
    public boolean hasAsset(UUID bookId, BookAssetType type) {
        return assets.existsByBookIdAndAssetType(bookId, type);
    }

    @Transactional(readOnly = true)
    public void assertPdfAccess(UUID bookId, String email) {
        BookAsset asset = findAsset(bookId, BookAssetType.PDF);
        if (!canAccess(asset.getAccessLevel(), findUser(email).getRole())) throw new AccessDeniedException("Acesso negado.");
    }

    private void validateRequest(AssetUploadRequest request) {
        if (!request.rightsConfirmed()) throw new BusinessException("Confirme que possui autorização para distribuir o arquivo.");
        if (request.fileSize() <= 0) throw new BusinessException("O arquivo está vazio.");
        String type = request.contentType().toLowerCase(Locale.ROOT);
        if (request.assetType() == BookAssetType.PDF) {
            if (!type.equals("application/pdf")) throw new BusinessException("O livro digital deve ser um arquivo PDF.");
            if (request.fileSize() > properties.maxPdfBytes()) throw new BusinessException("O PDF excede o limite permitido.");
        } else {
            if (!COVER_TYPES.contains(type)) throw new BusinessException("A capa deve ser JPEG, PNG ou WebP.");
            if (request.fileSize() > properties.maxCoverBytes()) throw new BusinessException("A imagem de capa excede o limite permitido.");
        }
    }

    private void validateSignature(BookUploadSession session, byte[] prefix) {
        boolean valid;
        if (session.getAssetType() == BookAssetType.PDF) {
            valid = new String(prefix, 0, Math.min(prefix.length, 5), StandardCharsets.US_ASCII).startsWith("%PDF-");
        } else if (session.getContentType().equals("image/png")) {
            byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            valid = startsWith(prefix, png);
        } else if (session.getContentType().equals("image/jpeg")) {
            valid = prefix.length >= 3 && (prefix[0] & 0xff) == 0xff && (prefix[1] & 0xff) == 0xd8 && (prefix[2] & 0xff) == 0xff;
        } else {
            valid = prefix.length >= 12
                    && new String(prefix, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(prefix, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
        }
        if (!valid) {
            storageAvailability.required().deleteQuietly(session.getObjectKey());
            session.setStatus(UploadStatus.CANCELLED);
            throw new BusinessException("A assinatura interna do arquivo é inválida.");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }

    private boolean canAccess(DigitalAccessLevel level, Role role) {
        return switch (level) {
            case PUBLIC, MEMBERS_ONLY -> true;
            case STAFF_ONLY -> role == Role.ADMIN || role == Role.LIBRARIAN;
            case ADMIN_ONLY -> role == Role.ADMIN;
        };
    }

    private BookAsset findAsset(UUID bookId, BookAssetType type) {
        findBook(bookId);
        return assets.findByBookIdAndAssetType(bookId, type)
                .orElseThrow(() -> new NotFoundException(type == BookAssetType.PDF
                        ? "Este livro não possui PDF cadastrado." : "Este livro não possui capa cadastrada."));
    }

    private Book findBook(UUID id) {
        return books.findById(id).filter(Book::isActive)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado."));
    }

    private UserAccount findUser(String email) {
        return users.findByEmailIgnoreCase(email).filter(UserAccount::isActive)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private BookAssetResponse toResponse(BookAsset asset) {
        return new BookAssetResponse(asset.getId(), asset.getBook().getId(), asset.getAssetType(),
                asset.getOriginalFilename(), asset.getContentType(), asset.getFileSize(), asset.getEtag(),
                asset.getAccessLevel(), asset.isDownloadAllowed(), asset.getUploadedBy().getName(),
                asset.getCreatedAt(), asset.getUpdatedAt());
    }

    private String sanitizeFilename(String filename) {
        String clean = filename == null ? "arquivo" : filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._ -]", "_").trim();
        if (clean.isBlank()) clean = "arquivo";
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private String extensionFor(BookAssetType type, String contentType, String filename) {
        if (type == BookAssetType.PDF) return "pdf";
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String stripQuotes(String value) {
        return value == null ? null : value.replace("\"", "");
    }
}
