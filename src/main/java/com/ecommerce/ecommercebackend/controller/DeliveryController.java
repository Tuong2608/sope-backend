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

import java.util.List;

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

    /**
     * Returns every active shipping option configured for the supplied province.
     * Checkout uses this endpoint so Admin shipping changes are reflected without
     * hard-coding STANDARD/EXPRESS on the frontend.
     */
    @PostMapping("/options")
    public ResponseEntity<List<DeliveryEstimateResponse>> options(
            @Valid @RequestBody DeliveryEstimateRequest request) {
        return ResponseEntity.ok(deliveryEstimateService.estimateOptions(request));
    }
}
