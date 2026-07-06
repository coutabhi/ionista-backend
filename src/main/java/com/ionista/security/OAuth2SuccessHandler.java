package com.ionista.security;

import com.ionista.entity.OAuthExchangeCode;
import com.ionista.entity.User;
import com.ionista.repository.OAuthExchangeCodeRepository;
import com.ionista.service.OAuthUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final long EXCHANGE_CODE_TTL_SECONDS = 60;

    private final OAuthUserService oAuthUserService;
    private final OAuthExchangeCodeRepository oAuthExchangeCodeRepository;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        User user = oAuthUserService.findOrCreateGoogleUser(
                email, providerId, firstName, lastName, Boolean.TRUE.equals(emailVerified));

        String code = UUID.randomUUID().toString();
        OAuthExchangeCode exchangeCode = OAuthExchangeCode.builder()
                .code(code)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(EXCHANGE_CODE_TTL_SECONDS))
                .used(false)
                .build();
        oAuthExchangeCodeRepository.save(exchangeCode);

        String redirectUrl = frontendRedirectUri + "?code=" + code;
        response.sendRedirect(redirectUrl);
    }
}
