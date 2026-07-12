package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request payload for creating/updating a delivery address (task C01).
 * Every field is required — a partial address cannot be saved.
 */
@Data
public class AddressRequest {

    @NotBlank(message = "recipientName is required")
    private String recipientName;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "phone must be a valid Vietnamese phone number")
    private String phone;

    @NotBlank(message = "province is required")
    private String province;

    @NotBlank(message = "district is required")
    private String district;

    @NotBlank(message = "ward is required")
    private String ward;

    @NotBlank(message = "addressDetail is required")
    private String addressDetail;

    /** If true, this address becomes the default (unsetting any previous one). */
    private boolean isDefault;
}
