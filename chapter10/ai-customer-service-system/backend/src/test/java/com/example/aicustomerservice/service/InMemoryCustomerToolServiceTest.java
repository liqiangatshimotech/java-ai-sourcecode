package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.entity.RefundAssessment;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 退款工具的结构化结论测试。
 *
 * 种子订单：SO202606210001 拣货中、SO202606210002 已发货、SO202606210003 已签收的日用品、
 * SO202606210004 退款复核中。护栏只读 requiresHumanReview，所以这里把每种状态的取值固定下来。
 */
class InMemoryCustomerToolServiceTest {

    private final InMemoryCustomerToolService toolService =
        new InMemoryCustomerToolService(new InMemoryOrderDao(Clock.systemUTC()));

    @Test
    void pickingShippedAndRefundingOrdersShouldRequireHumanReview() {
        // 拣货中：需要仓库确认能否拦截。
        RefundAssessment picking = toolService.assessRefund("C10001", "SO202606210001");
        // 已发货：需要确认物流能否拦截。
        RefundAssessment shipped = toolService.assessRefund("C10002", "SO202606210002");
        // 退款复核中：已经在人工流程里，AI 不能自行给结论。
        RefundAssessment refunding = toolService.assessRefund("C10001", "SO202606210004");

        assertThat(picking.requiresHumanReview()).isTrue();
        assertThat(shipped.requiresHumanReview()).isTrue();
        assertThat(refunding.requiresHumanReview()).isTrue();
        assertThat(picking.canAutoRefund()).isFalse();
        assertThat(shipped.canAutoRefund()).isFalse();
        assertThat(refunding.canAutoRefund()).isFalse();
    }

    @Test
    void ruleRejectedAndUnknownOrdersShouldBeDistinguishedFromHumanReview() {
        // 日用品是规则明确拒绝：不能退，但也不需要人工确认。
        RefundAssessment dailyNecessities = toolService.assessRefund("C10003", "SO202606210003");
        assertThat(dailyNecessities.canAutoRefund()).isFalse();
        assertThat(dailyNecessities.requiresHumanReview()).isFalse();

        // 用别人的客户编号查订单等同于查不到，交给人工核对身份和订单号。
        RefundAssessment notOwned = toolService.assessRefund("C10003", "SO202606210001");
        assertThat(notOwned.canAutoRefund()).isFalse();
        assertThat(notOwned.requiresHumanReview()).isTrue();
    }
}
