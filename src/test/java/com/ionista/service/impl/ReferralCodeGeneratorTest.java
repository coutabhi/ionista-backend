package com.ionista.service.impl;

import com.ionista.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralCodeGeneratorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReferralCodeGenerator referralCodeGenerator;

    @Test
    void generateUnique_returnsEightCharacterCode_whenFirstCandidateIsUnique() {
        when(userRepository.existsByReferralCode(anyString())).thenReturn(false);

        String code = referralCodeGenerator.generateUnique();

        assertThat(code).hasSize(8);
        verify(userRepository, times(1)).existsByReferralCode(anyString());
    }

    @Test
    void generateUnique_retries_whenCandidateAlreadyExists() {
        when(userRepository.existsByReferralCode(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        String code = referralCodeGenerator.generateUnique();

        assertThat(code).hasSize(8);
        verify(userRepository, times(3)).existsByReferralCode(anyString());
    }
}
