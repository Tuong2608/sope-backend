package com.ecommerce.ecommercebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String name;
    private String category;
    private String brand;
    private Long price;
    private Long oldPrice;
    private String description;
    private String imgUrl;
    private String url;
    private Map<String, String> specs;
}
