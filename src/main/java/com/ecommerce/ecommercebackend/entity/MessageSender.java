package com.ecommerce.ecommercebackend.entity;

/** Người gửi một tin nhắn trong phiên chat. */
public enum MessageSender {
    /** Khách hàng. */
    USER,
    /** Chatbot AI (Gemini). */
    AI,
    /** Nhân viên bán hàng (người thật). */
    SELLER
}
