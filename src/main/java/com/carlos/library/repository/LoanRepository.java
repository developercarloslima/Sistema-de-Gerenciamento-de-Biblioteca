package com.carlos.library.repository;

import com.carlos.library.domain.entity.Loan;
import com.carlos.library.domain.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    long countByMemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);
    boolean existsByMemberIdAndStatus(UUID memberId, LoanStatus status);
    boolean existsByMemberIdAndStatusAndDueDateBefore(UUID memberId, LoanStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"member", "member.user", "bookCopy", "bookCopy.book"})
    Optional<Loan> findDetailedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l join fetch l.member m join fetch m.user join fetch l.bookCopy c join fetch c.book where l.id = :id")
    Optional<Loan> findDetailedForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"member", "member.user", "bookCopy", "bookCopy.book"})
    Page<Loan> findByMemberId(UUID memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user", "bookCopy", "bookCopy.book"})
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user", "bookCopy", "bookCopy.book"})
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"member", "member.user", "bookCopy", "bookCopy.book"})
    List<Loan> findByStatusAndDueDateBetween(LoanStatus status, LocalDate start, LocalDate end);

    @Query(value = "select l from Loan l join fetch l.member m join fetch m.user join fetch l.bookCopy c join fetch c.book",
           countQuery = "select count(l) from Loan l")
    Page<Loan> findAllDetailed(Pageable pageable);
}
