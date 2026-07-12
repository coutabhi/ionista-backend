package com.ionista.dto.request;

import com.ionista.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Size(max = 100, message = "Tracking number must be at most 100 characters")
    private String trackingNumber;

    @Size(max = 100, message = "Carrier must be at most 100 characters")
    private String trackingCarrier;

    @Size(max = 500, message = "Tracking URL must be at most 500 characters")
    private String trackingUrl;
}
