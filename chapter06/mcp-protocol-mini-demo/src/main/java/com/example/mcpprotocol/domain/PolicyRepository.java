package com.example.mcpprotocol.domain;

import java.util.Map;
import java.util.Optional;

public final class PolicyRepository {

    private final Map<String, String> refundPolicies = Map.of(
        "acme", """
            ACME 退款政策：
            1. 已支付订单可在 7 天内申请退款。
            2. 已发货订单需要先完成退货入库。
            3. 企业客户补偿金额必须由主管审批。
            """,
        "beta", """
            BETA 退款政策：
            1. 订阅订单按剩余周期折算。
            2. 年付订单需要客户成功团队复核。
            """
    );

    public Optional<String> refundPolicy(String tenantId) {
        return Optional.ofNullable(refundPolicies.get(tenantId));
    }
}
