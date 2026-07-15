package com.carlos.library.controller;

import com.carlos.library.domain.enums.FineStatus;
import com.carlos.library.dto.ApiDtos.FineResponse;
import com.carlos.library.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {
    private final FineService service;

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    Page<FineResponse> list(@RequestParam(required=false) FineStatus status, Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/mine") @PreAuthorize("hasRole('MEMBER')")
    Page<FineResponse> mine(Authentication auth, Pageable pageable) { return service.mine(auth.getName(), pageable); }

    @PostMapping("/{id}/pay") @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    FineResponse pay(@PathVariable UUID id) { return service.pay(id); }

    @PostMapping("/{id}/cancel") @PreAuthorize("hasRole('ADMIN')")
    FineResponse cancel(@PathVariable UUID id) { return service.cancel(id); }
}
