package com.example.supportticketcopilot.service;

import com.example.supportticketcopilot.config.TicketCopilotProperties;
import com.example.supportticketcopilot.dto.ActionItem;
import com.example.supportticketcopilot.dto.AnalyzeTicketRequest;
import com.example.supportticketcopilot.dto.CustomerChannel;
import com.example.supportticketcopilot.dto.CustomerReplyDraft;
import com.example.supportticketcopilot.dto.CustomerTier;
import com.example.supportticketcopilot.dto.OwnerTeam;
import com.example.supportticketcopilot.dto.ReplyTone;
import com.example.supportticketcopilot.dto.TicketAnalysis;
import com.example.supportticketcopilot.dto.TicketCategory;
import com.example.supportticketcopilot.dto.TicketCopilotResponse;
import com.example.supportticketcopilot.dto.TicketPriority;
import jakarta.validation.Validation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketCopilotServiceTest {

    @Test
    void retriesWhenFirstStructuredOutputFailsValidation() {
        FakeTicketAiGateway gateway = new FakeTicketAiGateway(
            invalidAnalysis(),
            validBillingAnalysis()
        );
        TicketCopilotService service = createService(gateway, 3);

        TicketCopilotResponse response = service.analyze(new AnalyzeTicketRequest(
            "企业版客户反馈昨天扣费两次，发票也开不出来。",
            CustomerChannel.WEB,
            CustomerTier.ENTERPRISE
        ));

        assertThat(gateway.analyzeCalls()).isEqualTo(2);
        assertThat(gateway.lastAnalysisPrompt()).contains("上一次输出没有通过 Java 解析");
        assertThat(response.fallback()).isFalse();
        assertThat(response.attempts()).isEqualTo(2);
        assertThat(response.analysis().category()).isEqualTo(TicketCategory.BILLING);
        assertThat(response.analysis().priority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void returnsFallbackWhenAllAttemptsFailValidation() {
        FakeTicketAiGateway gateway = new FakeTicketAiGateway(
            invalidAnalysis(),
            invalidAnalysis()
        );
        TicketCopilotService service = createService(gateway, 2);

        TicketCopilotResponse response = service.analyze(new AnalyzeTicketRequest(
            "客户说系统一直打不开，但没有提供报错信息。",
            CustomerChannel.CHAT,
            CustomerTier.STANDARD
        ));

        assertThat(gateway.analyzeCalls()).isEqualTo(2);
        assertThat(response.fallback()).isTrue();
        assertThat(response.analysis().category()).isEqualTo(TicketCategory.OTHER);
        assertThat(response.analysis().summary()).isEqualTo("模型分析失败，转人工复核");
        assertThat(response.analysis().confidence()).isZero();
    }

    @Test
    void streamsCustomerReplyWithPromptContract() {
        FakeTicketAiGateway gateway = new FakeTicketAiGateway(validBillingAnalysis());
        TicketCopilotService service = createService(gateway, 2);

        StepVerifier.create(service.streamCustomerReply("发票开不出来", CustomerChannel.EMAIL))
            .expectNext("您好，", "我们已收到您的反馈。")
            .verifyComplete();

        assertThat(gateway.lastReplyPrompt())
            .contains("渠道：EMAIL")
            .contains("不承诺具体退款、赔偿或修复时间")
            .contains("发票开不出来");
    }

    private TicketCopilotService createService(FakeTicketAiGateway gateway, int maxAttempts) {
        return new TicketCopilotService(
            gateway,
            new TicketAnalysisValidator(Validation.buildDefaultValidatorFactory().getValidator()),
            new TicketCopilotProperties(maxAttempts)
        );
    }

    private static TicketAnalysis validBillingAnalysis() {
        return new TicketAnalysis(
            TicketCategory.BILLING,
            TicketPriority.HIGH,
            "企业客户遇到重复扣费和发票开具失败",
            List.of("订单号", "扣费时间", "发票抬头"),
            List.of(
                new ActionItem(
                    OwnerTeam.BILLING,
                    "核查重复扣费记录",
                    "查询支付流水并确认是否需要退款"
                ),
                new ActionItem(
                    OwnerTeam.CUSTOMER_SUPPORT,
                    "同步客户处理进展",
                    "向客户索取订单号和发票抬头"
                )
            ),
            new CustomerReplyDraft(
                "关于重复扣费和发票问题",
                "您好，我们已收到您反馈的重复扣费和发票开具问题。请补充订单号、扣费时间和发票抬头，我们会尽快核查支付流水并同步处理进展。",
                ReplyTone.APOLOGETIC
            ),
            0.86
        );
    }

    private static TicketAnalysis invalidAnalysis() {
        return new TicketAnalysis(
            TicketCategory.BILLING,
            TicketPriority.HIGH,
            "",
            List.of(),
            List.of(),
            null,
            1.2
        );
    }

    private static final class FakeTicketAiGateway implements TicketAiGateway {

        private final Queue<TicketAnalysis> responses = new ArrayDeque<>();
        private int analyzeCalls;
        private String lastAnalysisPrompt;
        private String lastReplyPrompt;

        private FakeTicketAiGateway(TicketAnalysis... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public TicketAnalysis analyzeTicket(String prompt) {
            analyzeCalls++;
            lastAnalysisPrompt = prompt;
            return responses.isEmpty() ? invalidAnalysis() : responses.remove();
        }

        @Override
        public Flux<String> streamCustomerReply(String prompt) {
            lastReplyPrompt = prompt;
            return Flux.just("您好，", "我们已收到您的反馈。");
        }

        int analyzeCalls() {
            return analyzeCalls;
        }

        String lastAnalysisPrompt() {
            return lastAnalysisPrompt;
        }

        String lastReplyPrompt() {
            return lastReplyPrompt;
        }
    }
}
