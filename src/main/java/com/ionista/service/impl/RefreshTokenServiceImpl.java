package com.ionista.service.impl;

import com.ionista.entity.RefreshToken;
import com.ionista.entity.User;
import com.ionista.exception.InvalidTokenException;
import com.ionista.repository.RefreshTokenRepository;
import com.ionista.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Override
    public String issueRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    public User validateAndConsume(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return refreshToken.getUser();
    }

    @Override
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
