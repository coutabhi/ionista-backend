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
import com.ionista.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ReferralCodeGenerator referralCodeGenerator;
    @Mock
    private OAuthExchangeCodeRepository oAuthExchangeCodeRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    private User buildUser() {
        User user = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .isActive(true)
                .provider(AuthProvider.LOCAL)
                .referralCode("ABC12345")
                .build();
        user.setId(1L);
        return user;
    }

    @Test
    void register_createsUser_whenEmailNotTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane@example.com").password("secret123")
                .build();

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(referralCodeGenerator.generateUnique()).thenReturn("ABC12345");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyRegistered() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane@example.com").password("secret123")
                .build();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throws_whenReferralCodeInvalid() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane@example.com").password("secret123")
                .referralCode("BADCODE1")
                .build();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.findByReferralCode("BADCODE1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid referral code");
    }

    @Test
    void register_linksReferrer_whenReferralCodeValid() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane@example.com").password("secret123")
                .referralCode("REF12345")
                .build();
        User referrer = buildUser();
        referrer.setId(99L);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.findByReferralCode("REF12345")).thenReturn(Optional.of(referrer));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(referralCodeGenerator.generateUnique()).thenReturn("NEWCODE1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(any(User.class))).thenReturn("refresh-token");

        authService.register(request);

        verify(userRepository).save(argThat(u -> u.getReferredByUserId().equals(99L)));
    }

    @Test
    void login_succeeds_withCorrectCredentials() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder().email("jane@example.com").password("secret123").build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_throws_whenUserNotFound() {
        LoginRequest request = LoginRequest.builder().email("missing@example.com").password("secret123").build();
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throws_whenPasswordWrong() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder().email("jane@example.com").password("wrong").build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throws_whenGoogleUserHasNoPassword() {
        User user = buildUser();
        user.setPassword(null);
        LoginRequest request = LoginRequest.builder().email("jane@example.com").password("secret123").build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throws_whenAccountInactive() {
        User user = buildUser();
        user.setActive(false);
        LoginRequest request = LoginRequest.builder().email("jane@example.com").password("secret123").build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void refresh_returnsNewTokenPair_whenValid() {
        User user = buildUser();
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();

        when(refreshTokenService.validateAndConsume("raw-token")).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(refreshTokenService.issueRefreshToken(user)).thenReturn("new-refresh");

        TokenPairResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
    }

    @Test
    void refresh_throws_whenUserInactive() {
        User user = buildUser();
        user.setActive(false);
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();
        when(refreshTokenService.validateAndConsume("raw-token")).thenReturn(user);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void logout_delegatesToRefreshTokenService() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();

        authService.logout(request);

        verify(refreshTokenService).revoke("raw-token");
    }

    @Test
    void exchangeOAuthCode_returnsTokens_whenCodeValidAndUnused() {
        User user = buildUser();
        OAuthExchangeCode code = OAuthExchangeCode.builder()
                .code("one-time-code")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(60))
                .used(false)
                .build();
        ExchangeCodeRequest request = ExchangeCodeRequest.builder().code("one-time-code").build();

        when(oAuthExchangeCodeRepository.findByCodeAndUsedFalse("one-time-code")).thenReturn(Optional.of(code));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issueRefreshToken(user)).thenReturn("refresh-token");

        TokenPairResponse response = authService.exchangeOAuthCode(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(code.isUsed()).isTrue();
        verify(oAuthExchangeCodeRepository).save(code);
    }

    @Test
    void exchangeOAuthCode_throws_whenCodeNotFound() {
        ExchangeCodeRequest request = ExchangeCodeRequest.builder().code("missing").build();
        when(oAuthExchangeCodeRepository.findByCodeAndUsedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.exchangeOAuthCode(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void exchangeOAuthCode_throws_whenCodeExpired() {
        User user = buildUser();
        OAuthExchangeCode code = OAuthExchangeCode.builder()
                .code("expired-code")
                .user(user)
                .expiresAt(Instant.now().minusSeconds(60))
                .used(false)
                .build();
        ExchangeCodeRequest request = ExchangeCodeRequest.builder().code("expired-code").build();

        when(oAuthExchangeCodeRepository.findByCodeAndUsedFalse("expired-code")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.exchangeOAuthCode(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");

        verify(oAuthExchangeCodeRepository, never()).save(any());
    }
}
