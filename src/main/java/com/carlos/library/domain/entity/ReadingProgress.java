package com.carlos.library.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reading_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reading_progress_user_book", columnNames = {"user_id", "book_id"})
})
public class ReadingProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "total_pages", nullable = false)
    private int totalPages;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "first_accessed_at", nullable = false)
    private OffsetDateTime firstAccessedAt;

    @Column(name = "last_accessed_at", nullable = false)
    private OffsetDateTime lastAccessedAt;
}
