package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.AdminShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
public class AdminShippingController {

    private final AdminShippingService shippingService;

    @GetMapping("/methods")
    public ResponseEntity<List<AdminShippingService.MethodResponse>> methods() {
        return ResponseEntity.ok(shippingService.getMethods());
    }

    @GetMapping("/zones")
    public ResponseEntity<List<AdminShippingService.ZoneResponse>> zones() {
        return ResponseEntity.ok(shippingService.getZones());
    }

    @GetMapping("/rates")
    public ResponseEntity<List<AdminShippingService.RateResponse>> rates() {
        return ResponseEntity.ok(shippingService.getRates());
    }

    @PatchMapping("/methods/{id}/active")
    public ResponseEntity<AdminShippingService.MethodResponse> setMethodActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setMethodActive(id, active));
    }

    @PatchMapping("/zones/{id}/active")
    public ResponseEntity<AdminShippingService.ZoneResponse> setZoneActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setZoneActive(id, active));
    }

    @PatchMapping("/rates/{id}/active")
    public ResponseEntity<AdminShippingService.RateResponse> setRateActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setRateActive(id, active));
    }
}
