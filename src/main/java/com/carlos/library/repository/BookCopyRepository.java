package com.carlos.library.repository;

import com.carlos.library.domain.entity.BookCopy;
import com.carlos.library.domain.enums.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookCopyRepository extends JpaRepository<BookCopy, UUID> {

    boolean existsByInventoryCode(String inventoryCode);

    long countByBookId(UUID bookId);

    long countByBookIdAndStatus(
            UUID bookId,
            BookCopyStatus status
    );

    @EntityGraph(attributePaths = "book")
    Page<BookCopy> findByBookId(
            UUID bookId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT copy
        FROM BookCopy copy
        WHERE copy.id = :copyId
    """)
    Optional<BookCopy> findByIdForUpdate(
            @Param("copyId") UUID copyId
    );
}