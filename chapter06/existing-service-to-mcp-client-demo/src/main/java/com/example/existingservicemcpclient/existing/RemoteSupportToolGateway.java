package com.example.existingservicemcpclient.existing;

public interface RemoteSupportToolGateway {

    OrderStatusSnapshot queryOrderStatus(String orderId);

    RefundActionSnapshot suggestRefundAction(String orderId);
}
