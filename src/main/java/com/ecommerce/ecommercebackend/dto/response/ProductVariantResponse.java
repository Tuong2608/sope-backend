package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * B02/B03 – DTO trả về thông tin một phiên bản sản phẩm (variant).
 */
@Data
@Builder
public class ProductVariantResponse {

    private Long id;
    private String sku;

    // Màu
    private String colorName;
    private String colorHex;

    // Dung lượng
    private String storageName;

    // Giá
    private Long price;
    private Long oldPrice;

    // Ảnh
    private String imageUrl;

    // Tồn kho
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;

    // Trạng thái
    private boolean active;
    private boolean inStock;
}
