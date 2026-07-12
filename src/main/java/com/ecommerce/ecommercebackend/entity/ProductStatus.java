package com.ecommerce.ecommercebackend.entity;

/**
 * Trạng thái kinh doanh của sản phẩm.
 *
 * <ul>
 *   <li>{@link #ACTIVE}   – đang bán, hiển thị trên storefront.</li>
 *   <li>{@link #INACTIVE} – ngừng bán, ẩn khỏi storefront.</li>
 *   <li>{@link #OUT_OF_STOCK} – tạm hết hàng (stockQuantity = 0), vẫn hiển thị nhưng không thể thêm giỏ.</li>
 * </ul>
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK
}
