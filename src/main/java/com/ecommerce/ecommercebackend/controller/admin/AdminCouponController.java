package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.request.CouponRequest;
import com.ecommerce.ecommercebackend.dto.response.CouponResponse;
import com.ecommerce.ecommercebackend.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin coupon management (task D03). Secured to ROLE_ADMIN via
 * {@code /api/admin/**} in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    /** Lists coupons, optionally filtered by {@code ?active=}. */
    @GetMapping
    public ResponseEntity<List<CouponResponse>> all(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(couponService.getAll(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> one(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        CouponResponse created = couponService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<CouponResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.setActive(id, true));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<CouponResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.setActive(id, false));
    }
}
