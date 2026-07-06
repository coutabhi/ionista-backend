package com.ionista.dto.request;

import com.ionista.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
