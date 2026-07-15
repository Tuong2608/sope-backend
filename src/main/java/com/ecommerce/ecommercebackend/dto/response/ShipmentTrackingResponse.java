package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full shipment tracking detail — status plus its history (task C10).
 */
@Data
@Builder
public class ShipmentTrackingResponse {

    private String trackingNumber;
    private String carrierName;
    private ShipmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ShipmentTrackingEventResponse> events;
}
