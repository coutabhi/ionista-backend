package com.ionista.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    @NotBlank(message = "Size is required")
    @Size(max = 20, message = "Size cannot exceed 20 characters")
    private String size;

    @NotBlank(message = "Color is required")
    @Size(max = 40, message = "Color cannot exceed 40 characters")
    private String color;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price override cannot be negative")
    private BigDecimal priceOverride;

    @NotBlank(message = "SKU is required")
    @Size(max = 70, message = "SKU cannot exceed 70 characters")
    private String sku;
}
