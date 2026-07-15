package com.carlos.library.service;

import com.carlos.library.domain.entity.Book;
import com.carlos.library.domain.entity.ReadingProgress;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.dto.ApiDtos.ReadingProgressRequest;
import com.carlos.library.dto.ApiDtos.ReadingProgressResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.BookRepository;
import com.carlos.library.repository.ReadingProgressRepository;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {
    private final ReadingProgressRepository progressRepository;
    private final UserAccountRepository users;
    private final BookRepository books;
    private final DigitalBookService digitalBooks;

    @Transactional(readOnly = true)
    public ReadingProgressResponse get(UUID bookId, String email) {
        digitalBooks.assertPdfAccess(bookId, email);
        UserAccount user = findUser(email);
        return progressRepository.findByUserIdAndBookId(user.getId(), bookId)
                .map(this::toResponse)
                .orElse(new ReadingProgressResponse(null, bookId, 1, 0, BigDecimal.ZERO, false, null, null));
    }

    @Transactional
    public ReadingProgressResponse update(UUID bookId, ReadingProgressRequest request, String email) {
        digitalBooks.assertPdfAccess(bookId, email);
        if (request.totalPages() < 1) throw new BusinessException("O total de páginas deve ser maior que zero.");
        if (request.currentPage() < 1 || request.currentPage() > request.totalPages()) {
            throw new BusinessException("A página atual deve estar entre 1 e o total de páginas.");
        }
        UserAccount user = findUser(email);
        Book book = books.findById(bookId).filter(Book::isActive)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado."));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ReadingProgress progress = progressRepository.findByUserIdAndBookId(user.getId(), bookId).orElseGet(() -> {
            ReadingProgress created = new ReadingProgress();
            created.setUser(user);
            created.setBook(book);
            created.setFirstAccessedAt(now);
            return created;
        });
        progress.setCurrentPage(request.currentPage());
        progress.setTotalPages(request.totalPages());
        progress.setCompleted(request.completed() || request.currentPage() == request.totalPages());
        progress.setLastAccessedAt(now);
        return toResponse(progressRepository.save(progress));
    }

    private UserAccount findUser(String email) {
        return users.findByEmailIgnoreCase(email).filter(UserAccount::isActive)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private ReadingProgressResponse toResponse(ReadingProgress progress) {
        BigDecimal percentage = progress.getTotalPages() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(progress.getCurrentPage() * 100.0 / progress.getTotalPages())
                .setScale(1, RoundingMode.HALF_UP);
        return new ReadingProgressResponse(progress.getId(), progress.getBook().getId(),
                progress.getCurrentPage(), progress.getTotalPages(), percentage, progress.isCompleted(),
                progress.getFirstAccessedAt(), progress.getLastAccessedAt());
    }
}
