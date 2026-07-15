package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.CreateUserRequest;
import com.carlos.library.dto.ApiDtos.UserResponse;
import com.carlos.library.dto.ApiDtos.UserStatusRequest;

import java.util.UUID;
import com.carlos.library.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {
    private final StaffService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request);
    }

    @GetMapping
    Page<UserResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @PatchMapping("/{id}/status")
    UserResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UserStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
