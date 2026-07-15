package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.response.ShipmentTrackingResponse;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin view of a simulated shipment plus a manual "advance" demo action
 * (task C10). Secured to ROLE_ADMIN via {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/orders/{orderId}/tracking")
@RequiredArgsConstructor
public class AdminShipmentTrackingController {

    private final ShipmentTrackingService shipmentTrackingService;

    @GetMapping
    public ResponseEntity<ShipmentTrackingResponse> get(@PathVariable Long orderId) {
        return shipmentTrackingService.getByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order " + orderId + " has no shipment tracking yet."));
    }

    /** Demo action: moves the shipment to its next status. */
    @PutMapping("/advance")
    public ResponseEntity<ShipmentTrackingResponse> advance(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentTrackingService.advance(orderId));
    }
}
