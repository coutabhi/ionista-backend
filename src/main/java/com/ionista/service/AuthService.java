package com.ionista.service;

import com.ionista.dto.request.LoginRequest;
import com.ionista.dto.request.RegisterRequest;
import com.ionista.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
