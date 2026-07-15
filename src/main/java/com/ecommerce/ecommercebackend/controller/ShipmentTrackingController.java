package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.response.ShipmentTrackingResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.service.OrderService;
import com.ecommerce.ecommercebackend.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a customer follow their own order's simulated shipment (task C10).
 */
@RestController
@RequestMapping("/api/orders/{orderId}/tracking")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    private final OrderService orderService;
    private final ShipmentTrackingService shipmentTrackingService;

    @GetMapping
    public ResponseEntity<ShipmentTrackingResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        // Ownership check reuses the same lookup as viewing the order itself.
        orderService.getOrder(user, orderId);

        return shipmentTrackingService.getByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order " + orderId + " has no shipment tracking yet."));
    }
}
