package com.ionista.service.impl;

import com.ionista.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class ReferralCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateUnique() {
        String code;
        do {
            code = generateCandidate();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    private String generateCandidate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
