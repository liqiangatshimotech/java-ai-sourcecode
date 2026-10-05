package com.example.existingservicemcpclient.existing;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupportDeskService {

    private final RemoteSupportToolGateway supportTools;

    public SupportDeskService(RemoteSupportToolGateway supportTools) {
        this.supportTools = supportTools;
    }

    public SupportCaseAnswer answerRefundQuestion(SupportCaseRequest request) {
        OrderStatusSnapshot order = supportTools.queryOrderStatus(request.orderId());
        RefundActionSnapshot advice = supportTools.suggestRefundAction(request.orderId());

        String reply = """
            订单 %s 当前状态为 %s。%s
            处理建议：%s。%s
            如需继续推进，请根据内部审批规则确认是否需要人工复核。
            """.formatted(
            order.orderId(),
            order.status(),
            order.summary(),
            advice.decision(),
            advice.reason()
        );

        return new SupportCaseAnswer(
            order.orderId(),
            order.status(),
            advice.decision(),
            reply,
            List.of("query_order_status", "suggest_refund_action")
        );
    }
}
