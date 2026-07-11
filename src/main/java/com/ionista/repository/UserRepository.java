package com.ionista.repository;

import com.ionista.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);
    boolean existsByReferralCode(String referralCode);

    Optional<User> findByProviderAndProviderId(com.ionista.enums.AuthProvider provider, String providerId);

    @Query("select u from User u where :keyword is null or :keyword = '' " +
            "or lower(u.firstName) like lower(concat('%', :keyword, '%')) " +
            "or lower(u.lastName) like lower(concat('%', :keyword, '%')) " +
            "or lower(u.email) like lower(concat('%', :keyword, '%'))")
    Page<User> search(@Param("keyword") String keyword, Pageable pageable);

    List<User> findAllByIsActiveTrue();

}
