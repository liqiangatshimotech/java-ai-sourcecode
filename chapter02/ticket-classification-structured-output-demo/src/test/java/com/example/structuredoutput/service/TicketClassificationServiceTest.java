package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.TicketCategory;
import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.dto.TicketPriority;
import com.example.structuredoutput.model.AlwaysInvalidModelClient;
import com.example.structuredoutput.model.FakeModelClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketClassificationServiceTest {

    @Test
    void retriesWhenFirstModelOutputIsNotJson() {
        FakeModelClient modelClient = new FakeModelClient();
        TicketClassificationService service = DemoFactory.createService(modelClient, 3);

        TicketClassification result = service.classify(
            "昨天扣费两次，发票也开不出来，客户经理一直没回复。"
        );

        assertThat(modelClient.callCount()).isEqualTo(2);
        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
        assertThat(result.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(result.requiredData()).containsExactly("订单号", "扣费时间", "发票抬头");
        assertThat(result.confidence()).isEqualTo(0.86);
    }

    @Test
    void returnsFallbackWhenAllAttemptsFailValidation() {
        AlwaysInvalidModelClient modelClient = new AlwaysInvalidModelClient();
        TicketClassificationService service = DemoFactory.createService(modelClient, 2);

        TicketClassification result = service.classify(
            "昨天扣费两次，发票也开不出来，客户经理一直没回复。"
        );

        assertThat(modelClient.callCount()).isEqualTo(2);
        assertThat(result.category()).isEqualTo(TicketCategory.OTHER);
        assertThat(result.priority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(result.summary()).isEqualTo("模型分类失败，转人工处理");
        assertThat(result.confidence()).isZero();
    }
}
