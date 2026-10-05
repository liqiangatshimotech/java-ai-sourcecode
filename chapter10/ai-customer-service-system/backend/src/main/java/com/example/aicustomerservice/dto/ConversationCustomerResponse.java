package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.CustomerProfile;

/** 会话详情中的客户 360 基础信息。 */
public record ConversationCustomerResponse(
    String customerId,
    String displayName,
    String tier,
    String phoneMasked
) {
    public static ConversationCustomerResponse from(CustomerProfile customer) {
        return new ConversationCustomerResponse(
            customer.customerId(),
            customer.displayName(),
            customer.tier().name(),
            customer.phoneMasked()
        );
    }
}
