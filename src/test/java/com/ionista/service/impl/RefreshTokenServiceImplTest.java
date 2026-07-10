package com.ionista.service.impl;

import com.ionista.entity.RefreshToken;
import com.ionista.entity.User;
import com.ionista.exception.InvalidTokenException;
import com.ionista.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 2592000000L);
        user = User.builder().email("jane@example.com").build();
        user.setId(1L);
    }

    @Test
    void issueRefreshToken_savesHashedTokenAndReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.issueRefreshToken(user);

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(rawToken).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void issueRefreshToken_generatesDifferentTokensEachCall() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token1 = refreshTokenService.issueRefreshToken(user);
        String token2 = refreshTokenService.issueRefreshToken(user);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void validateAndConsume_returnsUser_whenTokenValid() {
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash("irrelevant-for-mock")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = refreshTokenService.validateAndConsume("raw-token");

        assertThat(result).isEqualTo(user);
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void validateAndConsume_throws_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume("unknown"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAndConsume_throws_whenTokenRevoked() {
        RefreshToken stored = RefreshToken.builder()
                .user(user).tokenHash("hash")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume("raw-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void validateAndConsume_throws_whenTokenExpired() {
        RefreshToken stored = RefreshToken.builder()
                .user(user).tokenHash("hash")
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume("raw-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void revoke_marksTokenRevoked_whenFound() {
        RefreshToken stored = RefreshToken.builder()
                .user(user).tokenHash("hash")
                .expiryDate(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        refreshTokenService.revoke("raw-token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revoke_doesNothing_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }
}
