package com.ionista.dto.request;

import com.ionista.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferRequest {

    @NotBlank(message = "Offer name is required")
    @Size(max = 150, message = "Offer name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be positive")
    private BigDecimal value;

    @NotNull(message = "Start time is required")
    private LocalDateTime startsAt;

    @NotNull(message = "End time is required")
    private LocalDateTime endsAt;

    private Boolean active;

    private List<Long> productIds;

    private List<Long> categoryIds;
}
