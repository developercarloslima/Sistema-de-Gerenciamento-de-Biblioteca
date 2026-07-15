package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.ProfileResponse;
import com.carlos.library.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping
    ProfileResponse me(Authentication authentication) {
        return service.me(authentication.getName());
    }
}
