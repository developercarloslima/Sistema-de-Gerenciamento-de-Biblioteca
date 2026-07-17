package com.carlos.library.service;

import com.carlos.library.config.LibraryProperties;
import com.carlos.library.domain.entity.BookCopy;
import com.carlos.library.domain.entity.Fine;
import com.carlos.library.domain.entity.Loan;
import com.carlos.library.domain.entity.Member;
import com.carlos.library.domain.entity.Reservation;
import com.carlos.library.domain.enums.BookCopyStatus;
import com.carlos.library.domain.enums.FineStatus;
import com.carlos.library.domain.enums.LoanStatus;
import com.carlos.library.domain.enums.MemberStatus;
import com.carlos.library.domain.enums.NotificationType;
import com.carlos.library.domain.enums.ReservationStatus;
import com.carlos.library.dto.ApiDtos.LoanCreateRequest;
import com.carlos.library.dto.ApiDtos.LoanResponse;
import com.carlos.library.dto.ApiDtos.ReturnResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.BookCopyRepository;
import com.carlos.library.repository.FineRepository;
import com.carlos.library.repository.LoanRepository;
import com.carlos.library.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final List<LoanStatus> OPEN_LOAN_STATUSES =
            List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final ReservationRepository reservations;
    private final FineRepository fines;
    private final MemberService memberService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final MapperService mapper;
    private final LibraryProperties properties;

    @Transactional
    public LoanResponse create(
            LoanCreateRequest request,
            String operatorEmail
    ) {
        Member member = memberService.find(request.memberId());

        validateEligibility(member);

        BookCopy copy = copies.findByIdForUpdate(request.bookCopyId())
                .orElseThrow(() ->
                        new NotFoundException("Exemplar não encontrado.")
                );

        boolean hasOpenLoan = loans.existsByBookCopyIdAndStatusIn(
                copy.getId(),
                OPEN_LOAN_STATUSES
        );

        if (hasOpenLoan) {
            throw new BusinessException(
                    "Este exemplar já possui um empréstimo ativo."
            );
        }

        if (copy.getStatus() == BookCopyStatus.RESERVED) {
            Reservation ready = reservations
                    .findByBookCopyIdAndMemberIdAndStatus(
                            copy.getId(),
                            member.getId(),
                            ReservationStatus.READY
                    )
                    .orElseThrow(() ->
                            new BusinessException(
                                    "Exemplar reservado para outro membro."
                            )
                    );

            if (
                    ready.getExpirationDate() != null
                            && ready.getExpirationDate().isBefore(
                            OffsetDateTime.now(ZoneOffset.UTC)
                    )
            ) {
                throw new BusinessException(
                        "O prazo desta reserva expirou; " +
                                "aguarde a liberação automática do exemplar."
                );
            }

            ready.setStatus(ReservationStatus.FULFILLED);

        } else if (copy.getStatus() == BookCopyStatus.AVAILABLE) {
            boolean hasWaitingReservations =
                    reservations.existsByBookIdAndStatus(
                            copy.getBook().getId(),
                            ReservationStatus.WAITING
                    );

            if (hasWaitingReservations) {
                throw new BusinessException(
                        "Há uma fila de reservas para este livro."
                );
            }

        } else {
            throw new BusinessException(
                    "Exemplar indisponível para empréstimo."
            );
        }

        int days = request.loanDays() == null
                ? properties.defaultLoanDays()
                : request.loanDays();

        if (days < 1 || days > 60) {
            throw new BusinessException(
                    "O prazo deve estar entre 1 e 60 dias."
            );
        }

        Loan loan = new Loan();

        loan.setMember(member);
        loan.setBookCopy(copy);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(days));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setCreatedBy(operatorEmail);

        copy.setStatus(BookCopyStatus.LOANED);

        Loan savedLoan = loans.saveAndFlush(loan);

        return mapper.loan(savedLoan);
    }

    @Transactional
    public ReturnResponse returnLoan(UUID id) {
        Loan loan = detailedForUpdate(id);

        if (
                loan.getStatus() != LoanStatus.ACTIVE
                        && loan.getStatus() != LoanStatus.OVERDUE
        ) {
            throw new BusinessException(
                    "Empréstimo já finalizado."
            );
        }

        LocalDate today = LocalDate.now();
        BigDecimal fineAmount = BigDecimal.ZERO;

        if (today.isAfter(loan.getDueDate())) {
            long lateDays = ChronoUnit.DAYS.between(
                    loan.getDueDate(),
                    today
            );

            fineAmount = properties.dailyFine()
                    .multiply(BigDecimal.valueOf(lateDays));

            createFineIfNecessary(
                    loan,
                    fineAmount,
                    lateDays
            );
        }

        loan.setReturnDate(today);
        loan.setStatus(LoanStatus.RETURNED);

        reservationService.allocateNextOrRelease(
                loan.getBookCopy(),
                loan.getBookCopy().getBook()
        );

        return new ReturnResponse(
                mapper.loan(loan),
                fineAmount
        );
    }

    @Transactional
    public LoanResponse renew(
            UUID id,
            Authentication auth
    ) {
        Loan loan = detailedForUpdate(id);

        boolean isMember = auth.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_MEMBER")
                );

        if (
                isMember
                        && !loan.getMember()
                        .getUser()
                        .getEmail()
                        .equalsIgnoreCase(auth.getName())
        ) {
            throw new NotFoundException(
                    "Empréstimo não encontrado."
            );
        }

        validateRenewalEligibility(loan.getMember());

        if (
                loan.getStatus() != LoanStatus.ACTIVE
                        || LocalDate.now().isAfter(loan.getDueDate())
        ) {
            throw new BusinessException(
                    "Empréstimo vencido ou finalizado " +
                            "não pode ser renovado."
            );
        }

        if (
                loan.getRenewalCount()
                        >= properties.maxRenewals()
        ) {
            throw new BusinessException(
                    "Limite de renovações atingido."
            );
        }

        boolean hasWaitingReservations =
                reservations.existsByBookIdAndStatus(
                        loan.getBookCopy()
                                .getBook()
                                .getId(),
                        ReservationStatus.WAITING
                );

        if (hasWaitingReservations) {
            throw new BusinessException(
                    "Livro possui reservas pendentes."
            );
        }

        loan.setDueDate(
                loan.getDueDate()
                        .plusDays(properties.defaultLoanDays())
        );

        loan.setRenewalCount(
                loan.getRenewalCount() + 1
        );

        return mapper.loan(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse get(
            UUID id,
            Authentication auth
    ) {
        Loan loan = detailed(id);

        boolean isMember = auth.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_MEMBER")
                );

        if (
                isMember
                        && !loan.getMember()
                        .getUser()
                        .getEmail()
                        .equalsIgnoreCase(auth.getName())
        ) {
            throw new NotFoundException(
                    "Empréstimo não encontrado."
            );
        }

        return mapper.loan(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> list(
            UUID memberId,
            LoanStatus status,
            Pageable pageable
    ) {
        if (memberId != null) {
            return loans.findByMemberId(
                    memberId,
                    pageable
            ).map(mapper::loan);
        }

        if (status != null) {
            return loans.findByStatus(
                    status,
                    pageable
            ).map(mapper::loan);
        }

        return loans.findAllDetailed(pageable)
                .map(mapper::loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> mine(
            String email,
            Pageable pageable
    ) {
        Member member = memberService.findByEmail(email);

        return loans.findByMemberId(
                member.getId(),
                pageable
        ).map(mapper::loan);
    }

    @Transactional
    public void processDeadlines() {
        LocalDate today = LocalDate.now();

        List<Loan> overdue =
                loans.findByStatusAndDueDateBefore(
                        LoanStatus.ACTIVE,
                        today
                );

        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);

            notificationService.createOnce(
                    loan.getMember().getUser(),
                    "Empréstimo atrasado",
                    "O empréstimo de '"
                            + loan.getBookCopy()
                            .getBook()
                            .getTitle()
                            + "' está atrasado.",
                    NotificationType.LOAN_OVERDUE,
                    loan.getId()
            );
        }

        LocalDate dueSoon = today.plusDays(
                properties.dueSoonDays()
        );

        List<Loan> upcoming =
                loans.findByStatusAndDueDateBetween(
                        LoanStatus.ACTIVE,
                        today,
                        dueSoon
                );

        for (Loan loan : upcoming) {
            notificationService.createOnce(
                    loan.getMember().getUser(),
                    "Prazo de devolução próximo",
                    "O livro '"
                            + loan.getBookCopy()
                            .getBook()
                            .getTitle()
                            + "' vence em "
                            + loan.getDueDate()
                            + ".",
                    NotificationType.LOAN_DUE_SOON,
                    loan.getId()
            );
        }
    }

    private void validateEligibility(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(
                    "Membro não está ativo."
            );
        }

        long current = loans.countByMemberIdAndStatusIn(
                member.getId(),
                OPEN_LOAN_STATUSES
        );

        if (current >= member.getMaximumLoans()) {
            throw new BusinessException(
                    "Limite de empréstimos atingido."
            );
        }

        if (hasOverdueLoan(member)) {
            throw new BusinessException(
                    "Membro possui empréstimo atrasado."
            );
        }

        BigDecimal pendingFines =
                fines.sumPendingByMemberId(member.getId());

        if (
                pendingFines.compareTo(
                        properties.maxUnpaidFine()
                ) > 0
        ) {
            throw new BusinessException(
                    "Membro possui multas pendentes " +
                            "acima do limite permitido."
            );
        }
    }

    private void validateRenewalEligibility(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(
                    "Membro não está ativo."
            );
        }

        if (hasOverdueLoan(member)) {
            throw new BusinessException(
                    "Membro possui empréstimo atrasado."
            );
        }

        BigDecimal pendingFines =
                fines.sumPendingByMemberId(member.getId());

        if (
                pendingFines.compareTo(
                        properties.maxUnpaidFine()
                ) > 0
        ) {
            throw new BusinessException(
                    "Membro possui multas pendentes " +
                            "acima do limite permitido."
            );
        }
    }

    private boolean hasOverdueLoan(Member member) {
        return loans.existsByMemberIdAndStatus(
                member.getId(),
                LoanStatus.OVERDUE
        ) || loans.existsByMemberIdAndStatusAndDueDateBefore(
                member.getId(),
                LoanStatus.ACTIVE,
                LocalDate.now()
        );
    }

    private void createFineIfNecessary(
            Loan loan,
            BigDecimal amount,
            long lateDays
    ) {
        if (
                amount.signum() <= 0
                        || fines.existsByLoanId(loan.getId())
        ) {
            return;
        }

        Fine fine = new Fine();

        fine.setMember(loan.getMember());
        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setReason(
                "Atraso de " + lateDays + " dia(s)."
        );
        fine.setStatus(FineStatus.PENDING);

        fines.save(fine);

        notificationService.createOnce(
                loan.getMember().getUser(),
                "Multa gerada",
                "Foi gerada uma multa de R$ "
                        + amount
                        + " pelo atraso do livro '"
                        + loan.getBookCopy()
                        .getBook()
                        .getTitle()
                        + "'.",
                NotificationType.FINE_CREATED,
                fine.getId()
        );
    }

    private Loan detailed(UUID id) {
        return loans.findDetailedById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Empréstimo não encontrado."
                        )
                );
    }

    private Loan detailedForUpdate(UUID id) {
        return loans.findDetailedForUpdate(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Empréstimo não encontrado."
                        )
                );
    }
}