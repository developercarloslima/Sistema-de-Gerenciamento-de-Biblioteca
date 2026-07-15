package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.BookResponse;
import com.carlos.library.service.DigitalBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digital-library")
@RequiredArgsConstructor
public class DigitalLibraryController {
    private final DigitalBookService digitalBooks;

    @GetMapping("/books")
    Page<BookResponse> catalog(@RequestParam(required = false) String query, Pageable pageable,
                               Authentication authentication) {
        return digitalBooks.catalog(query, pageable, authentication.getName());
    }
}
