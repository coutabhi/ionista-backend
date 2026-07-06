package com.ionista.service;

import com.ionista.dto.request.ExchangeCodeRequest;
import com.ionista.dto.request.LoginRequest;
import com.ionista.dto.request.RefreshTokenRequest;
import com.ionista.dto.request.RegisterRequest;
import com.ionista.dto.response.AuthResponse;
import com.ionista.dto.response.TokenPairResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenPairResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    TokenPairResponse exchangeOAuthCode(ExchangeCodeRequest request);
}
