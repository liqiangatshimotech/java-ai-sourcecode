package com.example.supportticketcopilot.service;

import com.example.supportticketcopilot.config.TicketCopilotProperties;
import com.example.supportticketcopilot.dto.ActionItem;
import com.example.supportticketcopilot.dto.AnalyzeTicketRequest;
import com.example.supportticketcopilot.dto.CustomerChannel;
import com.example.supportticketcopilot.dto.CustomerReplyDraft;
import com.example.supportticketcopilot.dto.OwnerTeam;
import com.example.supportticketcopilot.dto.ReplyTone;
import com.example.supportticketcopilot.dto.TicketAnalysis;
import com.example.supportticketcopilot.dto.TicketCategory;
import com.example.supportticketcopilot.dto.TicketCopilotResponse;
import com.example.supportticketcopilot.dto.TicketPriority;
import com.example.supportticketcopilot.prompt.TicketPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class TicketCopilotService {

    private static final Logger log = LoggerFactory.getLogger(TicketCopilotService.class);

    private final TicketAiGateway aiGateway;
    private final TicketAnalysisValidator validator;
    private final TicketCopilotProperties properties;

    public TicketCopilotService(
        TicketAiGateway aiGateway,
        TicketAnalysisValidator validator,
        TicketCopilotProperties properties
    ) {
        this.aiGateway = aiGateway;
        this.validator = validator;
        this.properties = properties;
    }

    public TicketCopilotResponse analyze(AnalyzeTicketRequest request) {
        String requestId = UUID.randomUUID().toString();
        String repairHint = "";
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                String prompt = TicketPromptBuilder.buildAnalysisPrompt(request, repairHint);
                TicketAnalysis analysis = aiGateway.analyzeTicket(prompt);
                validator.validate(analysis);

                log.info(
                    "ticket analysis completed requestId={} category={} priority={} attempts={} fallback=false",
                    requestId,
                    analysis.category(),
                    analysis.priority(),
                    attempt
                );

                return new TicketCopilotResponse(requestId, analysis, attempt, false);
            }
            catch (RuntimeException ex) {
                lastError = ex;
                repairHint = TicketPromptBuilder.buildRepairHint(ex);
                log.warn(
                    "ticket analysis attempt failed requestId={} attempt={} maxAttempts={} message={}",
                    requestId,
                    attempt,
                    properties.maxAttempts(),
                    ex.getClass().getSimpleName()
                );
            }
        }

        log.warn(
            "ticket analysis fallback requestId={} maxAttempts={} lastError={}",
            requestId,
            properties.maxAttempts(),
            lastError == null ? "unknown" : lastError.getClass().getSimpleName()
        );

        return new TicketCopilotResponse(
            requestId,
            fallbackAnalysis(),
            properties.maxAttempts(),
            true
        );
    }

    public Flux<String> streamCustomerReply(String content, CustomerChannel channel) {
        String prompt = TicketPromptBuilder.buildReplyPrompt(content, channel);
        return aiGateway.streamCustomerReply(prompt);
    }

    private TicketAnalysis fallbackAnalysis() {
        return new TicketAnalysis(
            TicketCategory.OTHER,
            TicketPriority.MEDIUM,
            "模型分析失败，转人工复核",
            List.of("人工复核", "原始工单内容"),
            List.of(new ActionItem(
                OwnerTeam.CUSTOMER_SUPPORT,
                "创建人工复核任务",
                "联系客户确认关键信息后再分派处理团队"
            )),
            new CustomerReplyDraft(
                "我们已收到你的问题",
                "您好，我们已经收到您的反馈。为了避免误判，当前问题将转由人工客服复核处理。请补充相关订单号、发生时间和页面截图，我们会尽快跟进。",
                ReplyTone.REASSURING
            ),
            0.0
        );
    }
}
