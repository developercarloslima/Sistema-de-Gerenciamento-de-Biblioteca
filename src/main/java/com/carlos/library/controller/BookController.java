package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService service;

    @GetMapping
    Page<BookResponse> search(@RequestParam(required=false) String query,
                              @RequestParam(required=false) String category,
                              @RequestParam(defaultValue="false") boolean availableOnly,
                              Pageable pageable) {
        return service.search(query, category, availableOnly, pageable);
    }

    @GetMapping("/{id}") BookResponse get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    BookResponse create(@Valid @RequestBody BookRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deactivate(@PathVariable UUID id) { service.deactivate(id); }

    @PostMapping("/{bookId}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    BookCopyResponse addCopy(@PathVariable UUID bookId, @Valid @RequestBody BookCopyRequest request) {
        return service.addCopy(bookId, request);
    }

    @GetMapping("/{bookId}/copies")
    Page<BookCopyResponse> listCopies(@PathVariable UUID bookId, Pageable pageable) {
        return service.listCopies(bookId, pageable);
    }

    @PatchMapping("/copies/{copyId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    BookCopyResponse updateCopyStatus(@PathVariable UUID copyId, @Valid @RequestBody BookCopyStatusRequest request) {
        return service.updateCopyStatus(copyId, request);
    }
}
