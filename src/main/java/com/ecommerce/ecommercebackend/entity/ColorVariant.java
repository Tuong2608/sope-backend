package com.ecommerce.ecommercebackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A colour option of a product (from the TGDD crawl {@code color_variants}).
 * Embedded as an element collection of {@link Product}.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorVariant {

    @JsonProperty("color_name")
    @Column(name = "color_name", length = 100)
    private String colorName;

    @JsonProperty("variant_url")
    @Column(name = "variant_url", length = 500)
    private String variantUrl;

    @JsonProperty("data_code")
    @Column(name = "data_code", length = 50)
    private String dataCode;

    @JsonProperty("data_color")
    @Column(name = "data_color", length = 50)
    private String dataColor;

    @JsonProperty("is_active")
    @Column(name = "is_active")
    private boolean active;
}
