package com.carlos.library.service;

import com.carlos.library.config.LibraryProperties;
import com.carlos.library.domain.entity.*;
import com.carlos.library.domain.enums.*;
import com.carlos.library.dto.ApiDtos.ReservationCreateRequest;
import com.carlos.library.dto.ApiDtos.ReservationResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservations;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final MapperService mapper;
    private final LibraryProperties properties;

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request, Authentication auth) {
        Member member = resolveMember(request.memberId(), auth);
        if (member.getStatus() != MemberStatus.ACTIVE) throw new BusinessException("Membro não está ativo.");
        Book book = books.findById(request.bookId()).filter(Book::isActive)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado."));
        if (copies.countByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE) > 0) {
            throw new BusinessException("Há exemplar disponível; realize o empréstimo diretamente.");
        }
        List<ReservationStatus> active = List.of(ReservationStatus.WAITING, ReservationStatus.READY);
        if (reservations.existsByBookIdAndMemberIdAndStatusIn(book.getId(), member.getId(), active)) {
            throw new BusinessException("O membro já possui reserva ativa para este livro.");
        }
        Reservation r = new Reservation();
        r.setMember(member); r.setBook(book); r.setReservationDate(OffsetDateTime.now(ZoneOffset.UTC));
        r.setStatus(ReservationStatus.WAITING);
        return mapper.reservation(reservations.save(r));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> list(ReservationStatus status, Pageable pageable) {
        return (status == null ? reservations.findAllDetailed(pageable) : reservations.findByStatus(status, pageable))
                .map(mapper::reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> mine(String email, Pageable pageable) {
        return reservations.findByMemberId(memberService.findByEmail(email).getId(), pageable).map(mapper::reservation);
    }

    @Transactional
    public void cancel(UUID id, Authentication auth) {
        Reservation r = reservations.findById(id).orElseThrow(() -> new NotFoundException("Reserva não encontrada."));
        boolean staff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        if (!staff && !r.getMember().getUser().getEmail().equalsIgnoreCase(auth.getName())) {
            throw new NotFoundException("Reserva não encontrada.");
        }
        if (r.getStatus() != ReservationStatus.WAITING && r.getStatus() != ReservationStatus.READY) {
            throw new BusinessException("Esta reserva não pode mais ser cancelada.");
        }
        BookCopy copy = r.getBookCopy();
        r.setStatus(ReservationStatus.CANCELLED);
        if (copy != null) allocateNextOrRelease(copy, r.getBook());
    }

    @Transactional
    public void allocateNextOrRelease(BookCopy copy, Book book) {
        reservations.findFirstByBookIdAndStatusOrderByCreatedAtAsc(book.getId(), ReservationStatus.WAITING)
                .ifPresentOrElse(next -> {
                    next.setStatus(ReservationStatus.READY);
                    next.setBookCopy(copy);
                    next.setExpirationDate(OffsetDateTime.now(ZoneOffset.UTC).plusHours(properties.reservationHoldHours()));
                    copy.setStatus(BookCopyStatus.RESERVED);
                    notificationService.createOnce(next.getMember().getUser(), "Reserva disponível",
                            "O livro '" + book.getTitle() + "' está disponível para retirada.",
                            NotificationType.RESERVATION_READY, next.getId());
                }, () -> copy.setStatus(BookCopyStatus.AVAILABLE));
    }

    @Transactional
    public void expireReadyReservations() {
        List<Reservation> expired = reservations.findByStatusAndExpirationDateBefore(
                ReservationStatus.READY, OffsetDateTime.now(ZoneOffset.UTC));
        for (Reservation r : expired) {
            r.setStatus(ReservationStatus.EXPIRED);
            notificationService.createOnce(r.getMember().getUser(), "Reserva expirada",
                    "O prazo de retirada do livro '" + r.getBook().getTitle() + "' expirou.",
                    NotificationType.RESERVATION_EXPIRED, r.getId());
            if (r.getBookCopy() != null) allocateNextOrRelease(r.getBookCopy(), r.getBook());
        }
    }

    private Member resolveMember(UUID requested, Authentication auth) {
        boolean staff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LIBRARIAN"));
        if (staff) {
            if (requested == null) throw new BusinessException("memberId é obrigatório para funcionários.");
            return memberService.find(requested);
        }
        return memberService.findByEmail(auth.getName());
    }
}
