package com.ionista.controller;

import com.ionista.dto.request.ExchangeCodeRequest;
import com.ionista.dto.request.LoginRequest;
import com.ionista.dto.request.RefreshTokenRequest;
import com.ionista.dto.request.RegisterRequest;
import com.ionista.dto.response.AuthResponse;
import com.ionista.dto.response.TokenPairResponse;
import com.ionista.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenPairResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<TokenPairResponse> exchangeOAuthCode(@Valid @RequestBody ExchangeCodeRequest request) {
        TokenPairResponse response = authService.exchangeOAuthCode(request);
        return ResponseEntity.ok(response);
    }

}
