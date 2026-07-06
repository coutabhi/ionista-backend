package com.ionista.service.impl;

import com.ionista.entity.User;
import com.ionista.enums.AuthProvider;
import com.ionista.enums.Role;
import com.ionista.exception.BadRequestException;
import com.ionista.repository.UserRepository;
import com.ionista.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthUserServiceImpl implements OAuthUserService {

    private final UserRepository userRepository;
    private final ReferralCodeGenerator referralCodeGenerator;

    @Override
    public User findOrCreateGoogleUser(String email, String providerId, String firstName, String lastName, boolean emailVerified) {
        return userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> linkOrCreate(email, providerId, firstName, lastName, emailVerified));
    }

    private User linkOrCreate(String email, String providerId, String firstName, String lastName, boolean emailVerified) {
        return userRepository.findByEmail(email)
                .map(existing -> linkExisting(existing, providerId, emailVerified))
                .orElseGet(() -> createNew(email, providerId, firstName, lastName));
    }

    private User linkExisting(User existing, String providerId, boolean emailVerified) {
        if (!emailVerified) {
            throw new BadRequestException("This email is already registered. Please log in with your password to link Google sign-in.");
        }
        existing.setProviderId(providerId);
        return userRepository.save(existing);
    }

    private User createNew(String email, String providerId, String firstName, String lastName) {
        User user = User.builder()
                .firstName(firstName == null || firstName.isBlank() ? "Google" : firstName)
                .lastName(lastName == null || lastName.isBlank() ? "User" : lastName)
                .email(email)
                .password(null)
                .role(Role.USER)
                .isActive(true)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .referralCode(referralCodeGenerator.generateUnique())
                .build();

        return userRepository.save(user);
    }
}
