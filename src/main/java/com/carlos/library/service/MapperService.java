package com.carlos.library.service;

import com.carlos.library.domain.entity.*;
import com.carlos.library.domain.enums.BookCopyStatus;
import com.carlos.library.domain.enums.BookAssetType;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.repository.BookCopyRepository;
import com.carlos.library.repository.BookAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MapperService {
    private final BookCopyRepository copies;
    private final BookAssetRepository assets;

    public BookResponse book(Book b) {
        return new BookResponse(b.getId(), b.getTitle(), b.getIsbn(), b.getAuthor(), b.getPublisher(),
                b.getPublicationYear(), b.getCategory(), b.getDescription(), b.isActive(),
                copies.countByBookId(b.getId()), copies.countByBookIdAndStatus(b.getId(), BookCopyStatus.AVAILABLE),
                assets.existsByBookIdAndAssetType(b.getId(), BookAssetType.PDF),
                assets.existsByBookIdAndAssetType(b.getId(), BookAssetType.COVER), b.getCreatedAt());
    }

    public BookCopyResponse copy(BookCopy c) {
        return new BookCopyResponse(c.getId(), c.getBook().getId(), c.getBook().getTitle(), c.getInventoryCode(),
                c.getStatus(), c.getAcquisitionDate());
    }

    public MemberResponse member(Member m) {
        return new MemberResponse(m.getId(), m.getUser().getId(), m.getUser().getName(), m.getUser().getEmail(),
                m.getRegistrationNumber(), m.getPhone(), m.getStatus(), m.getMaximumLoans(), m.getCreatedAt());
    }

    public LoanResponse loan(Loan l) {
        return new LoanResponse(l.getId(), l.getMember().getId(), l.getMember().getUser().getName(),
                l.getBookCopy().getId(), l.getBookCopy().getInventoryCode(), l.getBookCopy().getBook().getId(),
                l.getBookCopy().getBook().getTitle(), l.getLoanDate(), l.getDueDate(), l.getReturnDate(),
                l.getRenewalCount(), l.getStatus(), l.getCreatedBy());
    }

    public ReservationResponse reservation(Reservation r) {
        return new ReservationResponse(r.getId(), r.getMember().getId(), r.getMember().getUser().getName(),
                r.getBook().getId(), r.getBook().getTitle(), r.getBookCopy() == null ? null : r.getBookCopy().getId(),
                r.getReservationDate(), r.getExpirationDate(), r.getStatus());
    }

    public FineResponse fine(Fine f) {
        return new FineResponse(f.getId(), f.getMember().getId(), f.getMember().getUser().getName(),
                f.getLoan().getId(), f.getLoan().getBookCopy().getBook().getTitle(), f.getAmount(), f.getReason(),
                f.getStatus(), f.getPaidAt(), f.getCreatedAt());
    }

    public NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isRead(),
                n.getReferenceId(), n.getCreatedAt());
    }
}
