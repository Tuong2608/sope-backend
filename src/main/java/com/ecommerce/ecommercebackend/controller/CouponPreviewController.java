package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ApplyCouponPreviewRequest;
import com.ecommerce.ecommercebackend.dto.response.AvailableCouponResponse;
import com.ecommerce.ecommercebackend.dto.response.CouponPreviewResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.CouponPreviewService;
import com.ecommerce.ecommercebackend.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer-facing coupon endpoints.
 *
 * <ul>
 *   <li>GET /api/coupons/available?productId=... — public coupons relevant to a product.</li>
 *   <li>POST /api/coupons/apply-preview — validates a code against the logged-in user's cart.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponPreviewController {

    private final CouponPreviewService couponPreviewService;
    private final CouponService couponService;

    @GetMapping("/available")
    public ResponseEntity<List<AvailableCouponResponse>> available(
            @RequestParam Long productId) {
        return ResponseEntity.ok(couponService.getAvailableForProduct(productId));
    }

    @PostMapping("/apply-preview")
    public ResponseEntity<CouponPreviewResponse> applyPreview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ApplyCouponPreviewRequest request) {
        return ResponseEntity.ok(couponPreviewService.preview(user, request.getCouponCode()));
    }
}
