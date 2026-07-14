package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * B08 / B09 – Service quản lý tồn kho theo vòng đời đơn hàng.
 *
 * <h2>B08 – Giảm tồn kho khi đặt hàng thành công</h2>
 * <p>Khi một đơn hàng chuyển sang trạng thái PAID (thanh toán thành công),
 * {@link #deductStockForOrder(Order)} được gọi để giảm {@code stockQuantity}
 * của từng sản phẩm/variant trong đơn. Đồng thời tự động chuyển sản phẩm
 * sang {@code OUT_OF_STOCK} nếu còn 0 hàng.</p>
 *
 * <h2>B09 – Hoàn trả tồn kho khi hủy đơn</h2>
 * <p>Khi đơn hàng bị hủy (CANCELLED), {@link #restoreStockForOrder(Order)}
 * hoàn lại {@code stockQuantity} cho tất cả item trong đơn.
 * Nếu sản phẩm đang {@code OUT_OF_STOCK} sẽ được chuyển lại {@code ACTIVE}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;

    // ── B08: Giảm tồn kho khi đặt hàng thành công ───────────────────────────

    /**
     * B08 – Giảm stockQuantity cho từng item trong đơn hàng vừa được thanh toán.
     *
     * <p>Nếu {@link OrderItem#getVariantId()} khác null → trừ trên variant.
     * Nếu null → trừ trên product. Không để số âm.</p>
     *
     * @param order Đơn hàng đã được thanh toán (status = PAID)
     */
    @Transactional
    public void deductStockForOrder(Order order) {
        log.info("[B08] Giảm tồn kho cho đơn #{} ({})", order.getId(), order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            if (item.getVariantId() != null) {
                deductVariantStock(item.getVariantId(), item.getQuantity());
            } else if (item.getProductId() != null) {
                deductProductStock(item.getProductId(), item.getQuantity());
            }
        }
    }

    private void deductProductStock(Long productId, int qty) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) {
            log.warn("[B08] Product #{} không tìm thấy, bỏ qua", productId);
            return;
        }
        Product p = opt.get();
        int newStock = Math.max(0, p.getStockQuantity() - qty);
        p.setStockQuantity(newStock);
        // B08: Tự động chuyển OUT_OF_STOCK nếu hết hàng
        if (newStock == 0 && p.getStatus() == ProductStatus.ACTIVE) {
            p.setStatus(ProductStatus.OUT_OF_STOCK);
            log.info("[B08] Product #{} '{}' → OUT_OF_STOCK", productId, p.getName());
        }
        productRepository.save(p);
        log.debug("[B08] Product #{} stock: {} → {}", productId, p.getStockQuantity() + qty, newStock);
    }

    private void deductVariantStock(Long variantId, int qty) {
        Optional<ProductVariant> opt = variantRepository.findById(variantId);
        if (opt.isEmpty()) {
            log.warn("[B08] Variant #{} không tìm thấy, bỏ qua", variantId);
            return;
        }
        ProductVariant v = opt.get();
        int newStock = Math.max(0, v.getStockQuantity() - qty);
        v.setStockQuantity(newStock);
        if (newStock == 0) v.setActive(false);
        variantRepository.save(v);
        log.debug("[B08] Variant #{} stock: {} → {}", variantId, v.getStockQuantity() + qty, newStock);
    }

    // ── B09: Hoàn trả tồn kho khi hủy đơn ───────────────────────────────────

    /**
     * B09 – Hoàn lại stockQuantity khi đơn hàng bị hủy.
     *
     * <p>Nếu sản phẩm đang OUT_OF_STOCK và có hàng trở lại → chuyển về ACTIVE.</p>
     *
     * @param order Đơn hàng vừa bị hủy (status = CANCELLED)
     */
    @Transactional
    public void restoreStockForOrder(Order order) {
        log.info("[B09] Hoàn trả tồn kho cho đơn #{} ({})", order.getId(), order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            if (item.getVariantId() != null) {
                restoreVariantStock(item.getVariantId(), item.getQuantity());
            } else if (item.getProductId() != null) {
                restoreProductStock(item.getProductId(), item.getQuantity());
            }
        }
    }

    private void restoreProductStock(Long productId, int qty) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) {
            log.warn("[B09] Product #{} không tìm thấy khi hoàn trả", productId);
            return;
        }
        Product p = opt.get();
        int newStock = p.getStockQuantity() + qty;
        p.setStockQuantity(newStock);
        // B09: Hoàn trả → chuyển lại ACTIVE nếu đang OUT_OF_STOCK
        if (p.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
            p.setStatus(ProductStatus.ACTIVE);
            log.info("[B09] Product #{} '{}' → ACTIVE (hoàn trả {})", productId, p.getName(), qty);
        }
        productRepository.save(p);
        log.debug("[B09] Product #{} stock: {} → {}", productId, newStock - qty, newStock);
    }

    private void restoreVariantStock(Long variantId, int qty) {
        Optional<ProductVariant> opt = variantRepository.findById(variantId);
        if (opt.isEmpty()) {
            log.warn("[B09] Variant #{} không tìm thấy khi hoàn trả", variantId);
            return;
        }
        ProductVariant v = opt.get();
        v.setStockQuantity(v.getStockQuantity() + qty);
        if (!v.isActive() && v.getStockQuantity() > 0) v.setActive(true);
        variantRepository.save(v);
        log.debug("[B09] Variant #{} stock restored +{}", variantId, qty);
    }
}
