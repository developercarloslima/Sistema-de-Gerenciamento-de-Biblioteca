package com.carlos.library.repository;

import com.carlos.library.domain.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, UUID> {
    Optional<ReadingProgress> findByUserIdAndBookId(UUID userId, UUID bookId);
}
