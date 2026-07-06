package com.ionista.repository;

import com.ionista.entity.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    Page<LoyaltyTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
