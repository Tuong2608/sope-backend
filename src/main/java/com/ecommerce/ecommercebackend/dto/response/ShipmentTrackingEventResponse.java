package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One entry in a shipment's status history (task C10).
 */
@Data
@Builder
public class ShipmentTrackingEventResponse {

    private ShipmentStatus status;
    private String note;
    private LocalDateTime createdAt;
}
