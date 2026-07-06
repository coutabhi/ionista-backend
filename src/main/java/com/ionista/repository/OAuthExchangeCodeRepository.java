package com.ionista.repository;

import com.ionista.entity.OAuthExchangeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, Long> {

    Optional<OAuthExchangeCode> findByCodeAndUsedFalse(String code);
}
