package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Result of a delivery estimate: shipping fee plus the expected arrival window
 * (tasks C04/C05).
 */
@Data
@Builder
public class DeliveryEstimateResponse {

    private String zoneName;
    private String methodCode;
    private String methodName;
    /** Shipping fee in VND. */
    private Long fee;
    private LocalDate estimatedMinDate;
    private LocalDate estimatedMaxDate;
    /** Set when one or more items need restocking, explaining the extended window. */
    private String note;
}
