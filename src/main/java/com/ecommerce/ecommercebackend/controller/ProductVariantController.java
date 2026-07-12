package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ProductVariantRequest;
import com.ecommerce.ecommercebackend.dto.response.ProductVariantResponse;
import com.ecommerce.ecommercebackend.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B02/B03 – REST Controller quản lý phiên bản sản phẩm (variant).
 *
 * <ul>
 *   <li>GET  /api/products/{id}/variants        – lấy variants active (public)</li>
 *   <li>GET  /api/admin/products/{id}/variants  – lấy tất cả variants (admin)</li>
 *   <li>POST /api/admin/products/{id}/variants  – tạo variant mới (admin)</li>
 *   <li>PUT  /api/admin/variants/{variantId}    – cập nhật variant (admin)</li>
 *   <li>DELETE /api/admin/variants/{variantId}  – deactivate variant (admin)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    // ── Public: storefront ────────────────────────────────────────────────────

    /** B03 – Trả về các variant đang active + còn hàng cho storefront. */
    @GetMapping("/api/products/{productId}/variants")
    public ResponseEntity<List<ProductVariantResponse>> getActiveVariants(
            @PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getActiveVariants(productId));
    }

    // ── Admin only ────────────────────────────────────────────────────────────

    /** Lấy tất cả variants (kể cả inactive) — admin. */
    @GetMapping("/api/admin/products/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductVariantResponse>> getAllVariants(
            @PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getAllVariants(productId));
    }

    /** B02 – Tạo variant mới cho sản phẩm. */
    @PostMapping("/api/admin/products/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variantService.createVariant(productId, request));
    }

    /** B02 – Cập nhật thông tin variant. */
    @PutMapping("/api/admin/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(variantService.updateVariant(variantId, request));
    }

    /** B02 – Deactivate (xoá mềm) một variant. */
    @DeleteMapping("/api/admin/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateVariant(@PathVariable Long variantId) {
        variantService.deactivateVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
