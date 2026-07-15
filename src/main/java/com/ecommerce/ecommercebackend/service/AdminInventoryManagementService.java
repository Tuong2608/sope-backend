package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * H09 – Service quản lý tồn kho thủ công cho admin.
 *
 * <p>Cho phép admin:
 * <ul>
 *   <li>Nhập thêm hàng (restock) cho sản phẩm hoặc variant</li>
 *   <li>Đặt số lượng tồn kho tuyệt đối</li>
 *   <li>Thay đổi trạng thái sản phẩm (ACTIVE/INACTIVE/OUT_OF_STOCK)</li>
 *   <li>Cập nhật mức tồn tối thiểu (minStockLevel)</li>
 *   <li>Bulk restock nhiều sản phẩm cùng lúc</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInventoryManagementService {

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    /** Kết quả sau khi cập nhật tồn kho. */
    public record StockUpdateResult(
            Long    productId,
            Long    variantId,
            String  name,
            int     oldStock,
            int     newStock,
            int     delta,
            String  status
    ) {}

    /** Request nhập hàng cho một sản phẩm. */
    public record RestockRequest(
            Long productId,
            Long variantId,   // null nếu restock product
            int  quantity     // số lượng nhập thêm (> 0)
    ) {}

    // ── Restock ───────────────────────────────────────────────────────────────

    /**
     * H09 – Nhập thêm hàng cho một sản phẩm.
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng nhập thêm (phải > 0)
     */
    @Transactional
    public StockUpdateResult restockProduct(Long productId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Số lượng nhập phải > 0");

        Product p = findProductOrThrow(productId);
        int oldStock = p.getStockQuantity();
        p.setStockQuantity(oldStock + quantity);

        // H09: Nếu đang OUT_OF_STOCK mà được nhập hàng → tự động ACTIVE
        if (p.getStatus() == ProductStatus.OUT_OF_STOCK) {
            p.setStatus(ProductStatus.ACTIVE);
        }

        productRepository.save(p);
        log.info("[H09] Restock product #{} '{}': {} → {}", productId, p.getName(), oldStock, p.getStockQuantity());

        return new StockUpdateResult(productId, null, p.getName(),
                oldStock, p.getStockQuantity(), quantity, p.getStatus().name());
    }

    /**
     * H09 – Nhập thêm hàng cho một variant.
     */
    @Transactional
    public StockUpdateResult restockVariant(Long variantId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Số lượng nhập phải > 0");

        ProductVariant v = findVariantOrThrow(variantId);
        int oldStock = v.getStockQuantity();
        v.setStockQuantity(oldStock + quantity);
        if (!v.isActive()) v.setActive(true);

        variantRepository.save(v);
        log.info("[H09] Restock variant #{}: {} → {}", variantId, oldStock, v.getStockQuantity());

        String name = (v.getColorName() != null ? v.getColorName() : "") +
                      (v.getStorageName() != null ? " " + v.getStorageName() : "");
        return new StockUpdateResult(v.getProduct() != null ? v.getProduct().getId() : null,
                variantId, name.trim(), oldStock, v.getStockQuantity(), quantity, "ACTIVE");
    }

    /**
     * H09 – Đặt số lượng tồn kho tuyệt đối (ghi đè, không cộng).
     */
    @Transactional
    public StockUpdateResult setStock(Long productId, int newQuantity) {
        if (newQuantity < 0) throw new BadRequestException("Số lượng tồn kho không được âm");

        Product p = findProductOrThrow(productId);
        int oldStock = p.getStockQuantity();
        p.setStockQuantity(newQuantity);

        if (newQuantity == 0 && p.getStatus() == ProductStatus.ACTIVE) {
            p.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (newQuantity > 0 && p.getStatus() == ProductStatus.OUT_OF_STOCK) {
            p.setStatus(ProductStatus.ACTIVE);
        }

        productRepository.save(p);
        log.info("[H09] Set stock product #{}: {} → {}", productId, oldStock, newQuantity);

        return new StockUpdateResult(productId, null, p.getName(),
                oldStock, newQuantity, newQuantity - oldStock, p.getStatus().name());
    }

    /**
     * H09 – Cập nhật trạng thái sản phẩm thủ công.
     */
    @Transactional
    public StockUpdateResult updateStatus(Long productId, ProductStatus newStatus) {
        Product p = findProductOrThrow(productId);
        ProductStatus old = p.getStatus();
        p.setStatus(newStatus);
        productRepository.save(p);

        log.info("[H09] Status product #{} '{}': {} → {}", productId, p.getName(), old, newStatus);
        return new StockUpdateResult(productId, null, p.getName(),
                p.getStockQuantity(), p.getStockQuantity(), 0, newStatus.name());
    }

    /**
     * H09 – Cập nhật mức tồn tối thiểu.
     */
    @Transactional
    public StockUpdateResult updateMinStockLevel(Long productId, int minStockLevel) {
        if (minStockLevel < 0) throw new BadRequestException("minStockLevel không được âm");

        Product p = findProductOrThrow(productId);
        p.setMinStockLevel(minStockLevel);
        productRepository.save(p);

        log.info("[H09] MinStockLevel product #{} updated to {}", productId, minStockLevel);
        return new StockUpdateResult(productId, null, p.getName(),
                p.getStockQuantity(), p.getStockQuantity(), 0, p.getStatus().name());
    }

    /**
     * H09 – Bulk restock nhiều sản phẩm cùng lúc.
     *
     * @param requests Danh sách [{productId, variantId, quantity}]
     * @return Danh sách kết quả cho từng request
     */
    @Transactional
    public List<StockUpdateResult> bulkRestock(List<RestockRequest> requests) {
        log.info("[H09] Bulk restock {} items", requests.size());
        return requests.stream().map(req -> {
            if (req.variantId() != null) {
                return restockVariant(req.variantId(), req.quantity());
            } else {
                return restockProduct(req.productId(), req.quantity());
            }
        }).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private ProductVariant findVariantOrThrow(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
    }
}
