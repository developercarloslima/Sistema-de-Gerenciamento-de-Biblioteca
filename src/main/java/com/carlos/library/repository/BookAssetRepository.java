package com.carlos.library.repository;

import com.carlos.library.domain.entity.BookAsset;
import com.carlos.library.domain.enums.BookAssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookAssetRepository extends JpaRepository<BookAsset, UUID> {
    Optional<BookAsset> findByBookIdAndAssetType(UUID bookId, BookAssetType assetType);
    List<BookAsset> findByBookIdOrderByAssetType(UUID bookId);
    boolean existsByBookIdAndAssetType(UUID bookId, BookAssetType assetType);
}
