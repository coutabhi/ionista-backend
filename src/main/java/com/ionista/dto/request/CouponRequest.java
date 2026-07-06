package com.ionista.dto.request;

import com.ionista.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 40, message = "Coupon code cannot exceed 40 characters")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be positive")
    private BigDecimal value;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum order value cannot be negative")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum discount amount cannot be negative")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per-user limit must be at least 1")
    private Integer perUserLimit;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Boolean active;
}
