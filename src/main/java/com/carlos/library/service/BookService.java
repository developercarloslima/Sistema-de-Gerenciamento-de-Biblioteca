package com.carlos.library.service;

import com.carlos.library.domain.entity.Book;
import com.carlos.library.domain.entity.BookCopy;
import com.carlos.library.domain.enums.BookCopyStatus;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.BookCopyRepository;
import com.carlos.library.repository.BookRepository;
import com.carlos.library.repository.ReservationRepository;
import com.carlos.library.domain.enums.ReservationStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final ReservationRepository reservations;
    private final ReservationService reservationService;
    private final MapperService mapper;

    @Transactional
    public BookResponse create(BookRequest request) {
        if (books.existsByIsbn(request.isbn())) throw new BusinessException("ISBN já cadastrado.");
        Book b = new Book();
        apply(b, request);
        return mapper.book(books.save(b));
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> search(String query, String category, boolean availableOnly, boolean digitalOnly, Pageable pageable) {
        String q = blankToNull(query);
        String c = blankToNull(category);
        return books.search(q, c, availableOnly, digitalOnly, pageable).map(mapper::book);
    }

    @Transactional(readOnly = true)
    public BookResponse get(UUID id) { return mapper.book(find(id)); }

    @Transactional
    public BookResponse update(UUID id, BookRequest request) {
        Book b = find(id);
        if (!b.getIsbn().equals(request.isbn()) && books.existsByIsbn(request.isbn())) {
            throw new BusinessException("ISBN já cadastrado.");
        }
        apply(b, request);
        return mapper.book(b);
    }

    @Transactional
    public void deactivate(UUID id) {
        Book b = find(id);
        if (copies.countByBookIdAndStatus(id, BookCopyStatus.LOANED) > 0) {
            throw new BusinessException("Não é possível desativar um livro com exemplares emprestados.");
        }
        if (reservations.existsByBookIdAndStatusIn(id, List.of(ReservationStatus.WAITING, ReservationStatus.READY))) {
            throw new BusinessException("Não é possível desativar um livro com reservas ativas.");
        }
        b.setActive(false);
    }

    @Transactional
    public BookCopyResponse addCopy(UUID bookId, BookCopyRequest request) {
        if (copies.existsByInventoryCode(request.inventoryCode())) {
            throw new BusinessException("Código de inventário já cadastrado.");
        }
        BookCopy copy = new BookCopy();
        copy.setBook(find(bookId));
        copy.setInventoryCode(request.inventoryCode().trim().toUpperCase());
        copy.setAcquisitionDate(request.acquisitionDate());
        copy.setStatus(BookCopyStatus.AVAILABLE);
        return mapper.copy(copies.save(copy));
    }

    @Transactional(readOnly = true)
    public Page<BookCopyResponse> listCopies(UUID bookId, Pageable pageable) {
        find(bookId);
        return copies.findByBookId(bookId, pageable).map(mapper::copy);
    }

    @Transactional
    public BookCopyResponse updateCopyStatus(UUID copyId, BookCopyStatusRequest request) {
        BookCopy copy = copies.findById(copyId).orElseThrow(() -> new NotFoundException("Exemplar não encontrado."));
        if (copy.getStatus() == BookCopyStatus.LOANED || copy.getStatus() == BookCopyStatus.RESERVED) {
            throw new BusinessException("O status deste exemplar só pode ser alterado pelo fluxo de empréstimo ou reserva.");
        }
        if (request.status() == BookCopyStatus.LOANED || request.status() == BookCopyStatus.RESERVED) {
            throw new BusinessException("Use o fluxo de empréstimo ou reserva para definir esse status.");
        }
        if (request.status() == BookCopyStatus.AVAILABLE) {
            reservationService.allocateNextOrRelease(copy, copy.getBook());
        } else {
            copy.setStatus(request.status());
        }
        return mapper.copy(copy);
    }

    private Book find(UUID id) {
        return books.findById(id).filter(Book::isActive)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado."));
    }

    private void apply(Book b, BookRequest r) {
        b.setTitle(r.title().trim()); b.setIsbn(r.isbn().trim()); b.setAuthor(r.author().trim());
        b.setPublisher(r.publisher()); b.setPublicationYear(r.publicationYear()); b.setCategory(r.category().trim());
        b.setDescription(r.description()); b.setActive(true);
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
