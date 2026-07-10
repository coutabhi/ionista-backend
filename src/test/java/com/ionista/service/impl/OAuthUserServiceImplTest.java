package com.ionista.service.impl;

import com.ionista.entity.User;
import com.ionista.enums.AuthProvider;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthUserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReferralCodeGenerator referralCodeGenerator;

    @InjectMocks
    private OAuthUserServiceImpl oAuthUserService;

    @Test
    void findOrCreateGoogleUser_returnsExistingGoogleUser_whenAlreadyLinked() {
        User existing = User.builder().email("jane@example.com").provider(AuthProvider.GOOGLE).providerId("google-123").build();
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(existing));

        User result = oAuthUserService.findOrCreateGoogleUser("jane@example.com", "google-123", "Jane", "Doe", true);

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateGoogleUser_linksExistingLocalAccount_whenEmailVerified() {
        User existingLocal = User.builder().email("jane@example.com").provider(AuthProvider.LOCAL).build();
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingLocal));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthUserService.findOrCreateGoogleUser("jane@example.com", "google-123", "Jane", "Doe", true);

        assertThat(result.getProviderId()).isEqualTo("google-123");
        verify(userRepository).save(existingLocal);
    }

    @Test
    void findOrCreateGoogleUser_throws_whenLinkingUnverifiedEmail() {
        User existingLocal = User.builder().email("jane@example.com").provider(AuthProvider.LOCAL).build();
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingLocal));

        assertThatThrownBy(() -> oAuthUserService.findOrCreateGoogleUser("jane@example.com", "google-123", "Jane", "Doe", false))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateGoogleUser_createsNewUser_whenNoExistingAccount() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(referralCodeGenerator.generateUnique()).thenReturn("NEWCODE1");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthUserService.findOrCreateGoogleUser("new@example.com", "google-123", "New", "User", true);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getPassword()).isNull();
        assertThat(result.getReferralCode()).isEqualTo("NEWCODE1");
    }

    @Test
    void findOrCreateGoogleUser_fillsDefaultNames_whenGoogleProfileNamesBlank() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(referralCodeGenerator.generateUnique()).thenReturn("NEWCODE1");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = oAuthUserService.findOrCreateGoogleUser("new@example.com", "google-123", "", null, true);

        assertThat(result.getFirstName()).isEqualTo("Google");
        assertThat(result.getLastName()).isEqualTo("User");
    }
}
