package com.ecommerce.ecommercebackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A storage/configuration option of a product (from the TGDD crawl
 * {@code storage_variants}). Embedded as an element collection of
 * {@link Product}.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageVariant {

    @JsonProperty("storage_name")
    @Column(name = "storage_name", length = 100)
    private String storageName;

    @JsonProperty("variant_url")
    @Column(name = "variant_url", length = 500)
    private String variantUrl;

    @JsonProperty("is_active")
    @Column(name = "is_active")
    private boolean active;
}
