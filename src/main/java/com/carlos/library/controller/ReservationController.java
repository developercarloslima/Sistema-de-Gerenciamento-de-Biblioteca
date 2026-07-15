package com.carlos.library.controller;

import com.carlos.library.domain.enums.ReservationStatus;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.ReservationService;
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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse create(@Valid @RequestBody ReservationCreateRequest request, Authentication auth) {
        return service.create(request, auth);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    Page<ReservationResponse> list(@RequestParam(required=false) ReservationStatus status, Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('MEMBER')")
    Page<ReservationResponse> mine(Authentication auth, Pageable pageable) { return service.mine(auth.getName(), pageable); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID id, Authentication auth) { service.cancel(id, auth); }
}
