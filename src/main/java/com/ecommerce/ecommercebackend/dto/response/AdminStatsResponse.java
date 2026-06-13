package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregate dashboard figures for admins.
 */
@Data
@Builder
public class AdminStatsResponse {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalReviews;
    /** Revenue in VND from paid/completed orders. */
    private long totalRevenue;
}
