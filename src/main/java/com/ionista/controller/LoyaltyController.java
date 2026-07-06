package com.ionista.controller;

import com.ionista.common.SecurityUtils;
import com.ionista.dto.response.LoyaltyBalanceResponse;
import com.ionista.dto.response.LoyaltyTransactionResponse;
import com.ionista.dto.response.PageResponse;
import com.ionista.entity.User;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.UserRepository;
import com.ionista.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final UserRepository userRepository;

    @GetMapping("/balance")
    public ResponseEntity<LoyaltyBalanceResponse> balance() {
        return ResponseEntity.ok(loyaltyService.getBalance(currentUser()));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<LoyaltyTransactionResponse>> history(Pageable pageable) {
        return ResponseEntity.ok(loyaltyService.history(currentUser(), pageable));
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
