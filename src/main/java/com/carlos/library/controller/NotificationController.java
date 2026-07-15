package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.MessageResponse;
import com.carlos.library.dto.ApiDtos.NotificationResponse;
import com.carlos.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping Page<NotificationResponse> mine(Authentication auth, Pageable pageable) {
        return service.mine(auth.getName(), pageable);
    }

    @PatchMapping("/{id}/read") NotificationResponse read(@PathVariable UUID id, Authentication auth) {
        return service.markRead(id, auth.getName());
    }

    @PatchMapping("/read-all") MessageResponse readAll(Authentication auth) {
        int count = service.markAllRead(auth.getName());
        return new MessageResponse(count + " notificação(ões) marcada(s) como lida(s).");
    }
}
