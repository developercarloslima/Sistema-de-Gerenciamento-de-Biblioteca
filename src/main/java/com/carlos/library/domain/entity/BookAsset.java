package com.carlos.library.domain.entity;

import com.carlos.library.domain.enums.BookAssetType;
import com.carlos.library.domain.enums.DigitalAccessLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "book_assets", uniqueConstraints = {
        @UniqueConstraint(name = "uq_book_asset_type", columnNames = {"book_id", "asset_type"})
})
public class BookAsset extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private BookAssetType assetType;

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(length = 200)
    private String etag;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private DigitalAccessLevel accessLevel;

    @Column(name = "download_allowed", nullable = false)
    private boolean downloadAllowed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private UserAccount uploadedBy;
}
