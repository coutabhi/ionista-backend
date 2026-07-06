package com.ionista.repository;

import com.ionista.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);
    boolean existsByReferralCode(String referralCode);

    Optional<User> findByProviderAndProviderId(com.ionista.enums.AuthProvider provider, String providerId);

}
