package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.example.langgraph4j.aftersales.domain.RiskCheckResult;
import com.example.langgraph4j.aftersales.domain.RiskLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 退款风控示例：金额和账号标记共同决定风险等级。 */
@Service
public class RefundRiskService {

    public RiskCheckResult check(OrderSnapshot order) {
        if (order.suspiciousAccount() || order.paidAmount().compareTo(new BigDecimal("500")) >= 0) {
            return new RiskCheckResult(RiskLevel.HIGH, "账号或金额命中高风险规则");
        }
        if (order.paidAmount().compareTo(new BigDecimal("100")) >= 0) {
            return new RiskCheckResult(RiskLevel.MEDIUM, "退款金额超过 100 元，需人工确认");
        }
        return new RiskCheckResult(RiskLevel.LOW, "未命中风险规则");
    }
}
