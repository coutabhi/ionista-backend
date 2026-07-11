package com.ionista.service.impl;

import com.ionista.dto.request.ExchangeCodeRequest;
import com.ionista.dto.request.LoginRequest;
import com.ionista.dto.request.RefreshTokenRequest;
import com.ionista.dto.request.RegisterRequest;
import com.ionista.dto.response.AuthResponse;
import com.ionista.dto.response.TokenPairResponse;
import com.ionista.entity.OAuthExchangeCode;
import com.ionista.entity.User;
import com.ionista.enums.AuthProvider;
import com.ionista.enums.Role;
import com.ionista.exception.BadRequestException;
import com.ionista.exception.InvalidTokenException;
import com.ionista.repository.OAuthExchangeCodeRepository;
import com.ionista.repository.UserRepository;
import com.ionista.security.JwtService;
import com.ionista.service.AuthService;
import com.ionista.service.EmailService;
import com.ionista.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ReferralCodeGenerator referralCodeGenerator;
    private final OAuthExchangeCodeRepository oAuthExchangeCodeRepository;
    private final EmailService emailService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Long referredByUserId = null;
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            referredByUserId = userRepository.findByReferralCode(request.getReferralCode())
                    .map(User::getId)
                    .orElseThrow(() -> new BadRequestException("Invalid referral code"));
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .isActive(true)
                .provider(AuthProvider.LOCAL)
                .referralCode(referralCodeGenerator.generateUnique())
                .referredByUserId(referredByUserId)
                .build();

        User savedUser = userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(savedUser);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", savedUser.getEmail(), e);
        }

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.issueRefreshToken(savedUser);

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("User registered successfully")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if(!user.isActive()) {
            throw new BadRequestException("User account is inactive");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("Login successfull")
                .build();
    }

    @Override
    public TokenPairResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndConsume(request.getRefreshToken());

        if (!user.isActive()) {
            throw new BadRequestException("User account is inactive");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.issueRefreshToken(user);

        return TokenPairResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration)
                .build();
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    @Override
    public TokenPairResponse exchangeOAuthCode(ExchangeCodeRequest request) {
        OAuthExchangeCode exchangeCode = oAuthExchangeCodeRepository.findByCodeAndUsedFalse(request.getCode())
                .orElseThrow(() -> new InvalidTokenException("Invalid or already used exchange code"));

        if (exchangeCode.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Exchange code has expired");
        }

        exchangeCode.setUsed(true);
        oAuthExchangeCodeRepository.save(exchangeCode);

        User user = exchangeCode.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return TokenPairResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration)
                .build();
    }

}
