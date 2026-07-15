package com.carlos.library.controller;

import com.carlos.library.domain.enums.LoanStatus;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    LoanResponse create(@Valid @RequestBody LoanCreateRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    ReturnResponse returnLoan(@PathVariable UUID id) { return service.returnLoan(id); }

    @PostMapping("/{id}/renew")
    LoanResponse renew(@PathVariable UUID id, Authentication auth) { return service.renew(id, auth); }

    @GetMapping("/{id}") LoanResponse get(@PathVariable UUID id, Authentication auth) { return service.get(id, auth); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    Page<LoanResponse> list(@RequestParam(required=false) UUID memberId,
                            @RequestParam(required=false) LoanStatus status, Pageable pageable) {
        return service.list(memberId, status, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('MEMBER')")
    Page<LoanResponse> mine(Authentication auth, Pageable pageable) { return service.mine(auth.getName(), pageable); }
}
