package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.ColorVariant;
import com.ecommerce.ecommercebackend.entity.CrawledReview;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.entity.StorageVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response payload representing a single product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String mainThumbnail;
    private String name;
    private String category;
    private String brand;
    private String shortDescription;
    private String description;
    private Long price;
    private Long oldPrice;
    private String url;
    private String imgUrl;
    private List<String> images;
    private Map<String, String> specs;
    private List<StorageVariant> storageVariants;
    private List<ColorVariant> colorVariants;
    private List<CrawledReview> reviews;

    // ── B03: Inventory & status ───────────────────────────────────────────────

    /** Trạng thái kinh doanh: ACTIVE / INACTIVE / OUT_OF_STOCK */
    private ProductStatus status;

    /** Số lượng có thể bán = stockQuantity - reservedQuantity */
    private Integer availableQuantity;

    /** {@code true} nếu còn hàng và đang bán */
    private boolean inStock;

    /** {@code true} nếu hàng sắp hết (availableQuantity <= minStockLevel) */
    private boolean lowStock;

    /** B02/B03: Danh sách phiên bản (màu + dung lượng) đang active */
    private List<ProductVariantResponse> variants;
}
