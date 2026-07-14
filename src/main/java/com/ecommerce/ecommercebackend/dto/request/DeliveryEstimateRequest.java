package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for previewing shipping fee and delivery window (task C05).
 * {@code items} is optional — omit it (or leave empty) when only the shipping
 * zone/fee matters, e.g. a bare product page with a single item already known.
 */
@Data
public class DeliveryEstimateRequest {

    @NotBlank(message = "province is required")
    private String province;

    /** Shipping method code, e.g. "STANDARD" or "EXPRESS"; defaults to STANDARD. */
    private String methodCode = "STANDARD";

    @Valid
    private List<DeliveryEstimateItemRequest> items = new ArrayList<>();
}
