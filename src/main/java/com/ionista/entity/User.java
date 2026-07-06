package com.ionista.entity;

import com.ionista.common.BaseEntity;
import com.ionista.enums.AuthProvider;
import com.ionista.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private int loyaltyPoints = 0;

    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;

    @Column(name = "referred_by_user_id")
    private Long referredByUserId;
}
