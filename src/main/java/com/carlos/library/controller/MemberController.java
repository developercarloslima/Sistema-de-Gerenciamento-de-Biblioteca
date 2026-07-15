package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
public class MemberController {
    private final MemberService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    MemberResponse create(@Valid @RequestBody MemberCreateRequest request) { return service.create(request); }

    @GetMapping Page<MemberResponse> list(Pageable pageable) { return service.list(pageable); }
    @GetMapping("/{id}") MemberResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    MemberResponse update(@PathVariable UUID id, @Valid @RequestBody MemberUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    MemberResponse status(@PathVariable UUID id, @Valid @RequestBody MemberStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
