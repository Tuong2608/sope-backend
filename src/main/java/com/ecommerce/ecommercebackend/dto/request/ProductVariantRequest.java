package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * B02 – Request body để tạo hoặc cập nhật một ProductVariant.
 */
@Data
public class ProductVariantRequest {

    @NotBlank(message = "Tên màu không được để trống")
    private String colorName;

    private String colorHex;

    private String storageName;

    @Min(value = 0, message = "Giá không được âm")
    private Long price;

    private Long oldPrice;

    private String imageUrl;

    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stockQuantity = 0;

    private boolean active = true;
}
