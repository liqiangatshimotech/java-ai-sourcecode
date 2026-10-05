package com.example.springaimcpserver.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RefundPolicyService {

    private final Map<String, String> policies = Map.of(
        "acme", """
            # ACME 退款政策
            1. 已支付未发货订单可在 7 天内申请退款。
            2. 已发货订单需要先完成退货入库。
            3. 订单金额大于 500 元时，客服只能给出建议，最终补偿需主管审批。
            """,
        "beta", """
            # BETA 退款政策
            1. 订阅订单按剩余周期折算退款。
            2. 年付订单需客户成功团队复核。
            """
    );

    public String refundPolicy(String tenantId) {
        String policy = policies.get(tenantId);
        if (policy == null) {
            throw new IllegalArgumentException("未找到租户退款政策：" + tenantId);
        }
        return policy;
    }
}
