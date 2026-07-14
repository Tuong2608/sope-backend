package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ApplyCouponPreviewRequest;
import com.ecommerce.ecommercebackend.dto.response.CouponPreviewResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.CouponPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a logged-in customer try a coupon code before checkout (task D05).
 * Requires authentication — the coupon is evaluated against the caller's own cart.
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponPreviewController {

    private final CouponPreviewService couponPreviewService;

    @PostMapping("/apply-preview")
    public ResponseEntity<CouponPreviewResponse> applyPreview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ApplyCouponPreviewRequest request) {
        return ResponseEntity.ok(couponPreviewService.preview(user, request.getCouponCode()));
    }
}
