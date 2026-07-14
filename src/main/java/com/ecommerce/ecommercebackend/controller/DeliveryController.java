package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.DeliveryEstimateRequest;
import com.ecommerce.ecommercebackend.dto.response.DeliveryEstimateResponse;
import com.ecommerce.ecommercebackend.service.DeliveryEstimateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for previewing shipping fee and delivery window (task C05).
 * Callers (product page or cart) send a province + optional line items and get
 * back the fee and estimated arrival date range — no login required.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryEstimateService deliveryEstimateService;

    @PostMapping("/estimate")
    public ResponseEntity<DeliveryEstimateResponse> estimate(
            @Valid @RequestBody DeliveryEstimateRequest request) {
        return ResponseEntity.ok(deliveryEstimateService.estimate(request));
    }
}
