package com.carlos.library.repository;

import com.carlos.library.domain.entity.Reservation;
import com.carlos.library.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    boolean existsByBookIdAndMemberIdAndStatusIn(UUID bookId, UUID memberId, Collection<ReservationStatus> statuses);
    boolean existsByBookIdAndStatus(UUID bookId, ReservationStatus status);
    boolean existsByBookIdAndStatusIn(UUID bookId, Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"member", "member.user", "book", "bookCopy"})
    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAtAsc(UUID bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"member", "member.user", "book", "bookCopy"})
    Optional<Reservation> findByBookCopyIdAndMemberIdAndStatus(UUID bookCopyId, UUID memberId, ReservationStatus status);

    @EntityGraph(attributePaths = {"member", "member.user", "book", "bookCopy"})
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    @Query(value = "select r from Reservation r join fetch r.member m join fetch m.user join fetch r.book left join fetch r.bookCopy",
           countQuery = "select count(r) from Reservation r")
    Page<Reservation> findAllDetailed(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user", "book", "bookCopy"})
    Page<Reservation> findByMemberId(UUID memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user", "book", "bookCopy"})
    List<Reservation> findByStatusAndExpirationDateBefore(ReservationStatus status, OffsetDateTime now);
}
