package com.ionista.repository;

import com.ionista.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);
}
