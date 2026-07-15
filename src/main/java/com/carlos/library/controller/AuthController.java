package com.carlos.library.controller;

import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.service.AuthService;
import com.carlos.library.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) { return jwt.refresh(request.refreshToken()); }
}
