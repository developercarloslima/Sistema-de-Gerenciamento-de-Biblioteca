package com.carlos.library.repository;

import com.carlos.library.domain.entity.Fine;
import com.carlos.library.domain.enums.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface FineRepository extends JpaRepository<Fine, UUID> {
    @Query("select coalesce(sum(f.amount), 0) from Fine f where f.member.id = :memberId and f.status = com.carlos.library.domain.enums.FineStatus.PENDING")
    BigDecimal sumPendingByMemberId(@Param("memberId") UUID memberId);

    boolean existsByLoanId(UUID loanId);

    @EntityGraph(attributePaths = {"member", "member.user", "loan", "loan.bookCopy", "loan.bookCopy.book"})
    Page<Fine> findByMemberId(UUID memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user", "loan", "loan.bookCopy", "loan.bookCopy.book"})
    Page<Fine> findByStatus(FineStatus status, Pageable pageable);
}
