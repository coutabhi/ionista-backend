package com.ionista.dto.request;

import com.ionista.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base price cannot be negative")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount price cannot be negative")
    private BigDecimal discountPrice;

    @NotBlank(message = "SKU is required")
    @Size(max = 60, message = "SKU cannot exceed 60 characters")
    private String sku;

    @Size(max = 180, message = "Slug cannot exceed 180 characters")
    private String slug;

    private Boolean active;

    private Boolean featured;
}
