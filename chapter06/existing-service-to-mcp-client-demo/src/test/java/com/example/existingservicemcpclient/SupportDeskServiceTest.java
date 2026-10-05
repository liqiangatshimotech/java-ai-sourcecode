package com.example.existingservicemcpclient;

import com.example.existingservicemcpclient.existing.OrderStatusSnapshot;
import com.example.existingservicemcpclient.existing.RefundActionSnapshot;
import com.example.existingservicemcpclient.existing.RemoteSupportToolGateway;
import com.example.existingservicemcpclient.existing.SupportCaseAnswer;
import com.example.existingservicemcpclient.existing.SupportCaseRequest;
import com.example.existingservicemcpclient.existing.SupportDeskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportDeskServiceTest {

    @Test
    void keepsExistingServiceFlowAndDelegatesRemoteFactsToGateway() {
        SupportDeskService service = new SupportDeskService(new StaticRemoteSupportToolGateway());

        SupportCaseAnswer answer = service.answerRefundQuestion(new SupportCaseRequest(
            "ORD-1001",
            "客户询问这笔订单能不能退款"
        ));

        assertEquals("ORD-1001", answer.orderId());
        assertEquals("PAID", answer.orderStatus());
        assertEquals("APPROVE", answer.refundDecision());
        assertTrue(answer.toolsUsed().contains("query_order_status"));
        assertTrue(answer.toolsUsed().contains("suggest_refund_action"));
        assertFalse(answer.reply().contains("内部备注"));
    }

    private static class StaticRemoteSupportToolGateway implements RemoteSupportToolGateway {

        @Override
        public OrderStatusSnapshot queryOrderStatus(String orderId) {
            return new OrderStatusSnapshot(orderId, "PAID", true, "订单已支付且未发货。");
        }

        @Override
        public RefundActionSnapshot suggestRefundAction(String orderId) {
            return new RefundActionSnapshot(orderId, "APPROVE", false, "符合自动退款规则。");
        }
    }
}
