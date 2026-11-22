package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.LogisticsSnapshot;
import com.example.aicustomerservice.entity.OrderSnapshot;
import com.example.aicustomerservice.entity.RefundAssessment;
import java.util.Optional;

public interface CustomerToolService {

    // 订单工具同时校验 customerId 与 orderNo，避免查询到其他客户的订单。
    Optional<OrderSnapshot> findOrder(String customerId, String orderNo);

    // 物流工具只返回受控快照，不把承运商原始接口或凭证暴露给模型。
    Optional<LogisticsSnapshot> findLogistics(String orderNo);

    // 退款工具只做资格和下一步判断，不直接执行退款或修改订单状态。
    RefundAssessment assessRefund(String customerId, String orderNo);
}
