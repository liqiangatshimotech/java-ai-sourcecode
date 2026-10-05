package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.example.langgraph4j.aftersales.domain.PolicyCheckResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 确定性的退款政策检查；这类规则不应交给模型自由发挥。 */
@Service
public class RefundPolicyService {

    public PolicyCheckResult check(OrderSnapshot order) {
        if (order.alreadyRefunded()) {
            return new PolicyCheckResult(false, "该订单已退款，不能重复受理");
        }
        long deliveredDays = ChronoUnit.DAYS.between(order.deliveredDate(), LocalDate.now());
        if (deliveredDays > 7) {
            return new PolicyCheckResult(false, "已超过 7 天售后期");
        }
        return new PolicyCheckResult(true, "符合 7 天内退款政策");
    }
}
