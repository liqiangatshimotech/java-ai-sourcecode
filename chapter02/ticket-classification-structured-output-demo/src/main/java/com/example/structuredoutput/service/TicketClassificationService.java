package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.TicketCategory;
import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.dto.TicketPriority;
import com.example.structuredoutput.model.ModelClient;
import com.example.structuredoutput.prompt.TicketPromptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class TicketClassificationService {

    private final ModelClient modelClient;
    private final ObjectMapper objectMapper;
    private final TicketClassificationValidator validator;
    private final int maxAttempts;

    public TicketClassificationService(
        ModelClient modelClient,
        ObjectMapper objectMapper,
        TicketClassificationValidator validator,
        int maxAttempts
    ) {
        this.modelClient = modelClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.maxAttempts = maxAttempts;
    }

    public TicketClassification classify(String ticketText) {
        RuntimeException lastError = null;
        String repairHint = "";

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String rawOutput = "";

            try {
                String prompt = TicketPromptBuilder.build(ticketText, repairHint);
                rawOutput = modelClient.generate(prompt);

                TicketClassification result =
                    objectMapper.readValue(rawOutput, TicketClassification.class);

                validator.validate(result);
                return result;
            }
            catch (JsonProcessingException | IllegalArgumentException ex) {
                lastError = new IllegalStateException(
                    "第 " + attempt + " 次结构化输出失败，错误类型：" + ex.getClass().getSimpleName(),
                    ex
                );
                repairHint = buildRepairHint(ex);
                System.out.println(lastError.getClass().getSimpleName());
            }
        }

        return fallback(lastError);
    }

    private String buildRepairHint(Exception ex) {
        String message = ex.getClass().getSimpleName();
        if (message != null && message.length() > 300) {
            message = message.substring(0, 300);
        }

        return """
            上一次输出没有通过系统解析或校验。
            请修正输出，只返回合法 JSON。
            错误摘要：%s
            """.formatted(message == null ? "未知错误" : message);
    }

    private TicketClassification fallback(RuntimeException lastError) {
        if (lastError != null) {
            System.out.println("结构化输出多次失败，进入兜底流程：" + lastError.getCause().getClass().getSimpleName());
        }

        return new TicketClassification(
            TicketCategory.OTHER,
            TicketPriority.MEDIUM,
            "模型分类失败，转人工处理",
            List.of("人工复核", "原始工单内容"),
            0.0
        );
    }
}
