package com.carlos.library.repository;

import com.carlos.library.domain.entity.BookUploadSession;
import com.carlos.library.domain.enums.BookAssetType;
import com.carlos.library.domain.enums.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface BookUploadSessionRepository extends JpaRepository<BookUploadSession, UUID> {
    List<BookUploadSession> findByBookIdAndAssetTypeAndStatus(UUID bookId, BookAssetType assetType, UploadStatus status);
    List<BookUploadSession> findByStatusAndExpiresAtBefore(UploadStatus status, OffsetDateTime expiresAt);
}
