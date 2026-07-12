package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.ProductVariantRequest;
import com.ecommerce.ecommercebackend.dto.response.ProductVariantResponse;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * B02 – Service quản lý phiên bản sản phẩm (ProductVariant).
 *
 * <p>Mỗi variant là một tổ hợp màu + dung lượng của một Product,
 * với giá, ảnh và số lượng tồn kho độc lập.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository        productRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả variant đang active của một sản phẩm.
     * Dùng cho storefront (B03).
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveVariants(Long productId) {
        return variantRepository.findByProductIdAndActiveTrue(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy tất cả variant (kể cả inactive) — dùng cho admin.
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAllVariants(Long productId) {
        return variantRepository.findByProductId(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tạo một variant mới cho sản phẩm.
     *
     * @param productId ID sản phẩm cha
     * @param request   Thông tin variant
     */
    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        // Tạo SKU tự động nếu chưa có
        String sku = buildSku(product.getSku(), request.getColorName(), request.getStorageName());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .colorName(request.getColorName())
                .colorHex(request.getColorHex())
                .storageName(request.getStorageName())
                .price(request.getPrice())
                .oldPrice(request.getOldPrice())
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .active(request.isActive())
                .build();

        variant = variantRepository.save(variant);
        log.info("[VARIANT] Tạo variant #{} SKU={} cho product #{}", variant.getId(), sku, productId);
        return toResponse(variant);
    }

    /**
     * Cập nhật thông tin variant.
     */
    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        variant.setColorName(request.getColorName());
        variant.setColorHex(request.getColorHex());
        variant.setStorageName(request.getStorageName());
        variant.setPrice(request.getPrice());
        variant.setOldPrice(request.getOldPrice());
        variant.setImageUrl(request.getImageUrl());
        if (request.getStockQuantity() != null) {
            variant.setStockQuantity(request.getStockQuantity());
        }
        variant.setActive(request.isActive());

        variant = variantRepository.save(variant);
        log.info("[VARIANT] Cập nhật variant #{}", variantId);
        return toResponse(variant);
    }

    /**
     * Xoá mềm (deactivate) một variant — không xoá khỏi DB để bảo toàn lịch sử đơn hàng.
     */
    @Transactional
    public void deactivateVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
        variant.setActive(false);
        variantRepository.save(variant);
        log.info("[VARIANT] Deactivate variant #{}", variantId);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public ProductVariantResponse toResponse(ProductVariant v) {
        return ProductVariantResponse.builder()
                .id(v.getId())
                .sku(v.getSku())
                .colorName(v.getColorName())
                .colorHex(v.getColorHex())
                .storageName(v.getStorageName())
                .price(v.getPrice())
                .oldPrice(v.getOldPrice())
                .imageUrl(v.getImageUrl())
                .stockQuantity(v.getStockQuantity())
                .reservedQuantity(v.getReservedQuantity())
                .availableQuantity(v.getAvailableQuantity())
                .active(v.isActive())
                .inStock(v.isAvailable())
                .build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String buildSku(String productSku, String colorName, String storageName) {
        String base = productSku != null ? productSku : "PROD";
        String color = colorName != null
                ? colorName.toUpperCase().replaceAll("\\s+", "_").substring(0, Math.min(colorName.length(), 10))
                : "DEFAULT";
        String storage = storageName != null
                ? storageName.toUpperCase().replaceAll("\\s+", "")
                : "";
        return base + "-" + color + (storage.isEmpty() ? "" : "-" + storage);
    }
}
