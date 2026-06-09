package com.ecommerce.ecommercebackend.entity;

/**
 * Trạng thái của một phiên chat.
 *
 * <p>Theo thiết kế tuần 1: phân biệt khách đang chat với AI hay với người thật
 * (nhân viên bán hàng tiếp quản).</p>
 */
public enum ChatSessionStatus {
    /** Đang chat với chatbot AI. */
    CHATBOT,
    /** Đã chuyển cho nhân viên bán hàng (người thật) xử lý. */
    HUMAN
}
