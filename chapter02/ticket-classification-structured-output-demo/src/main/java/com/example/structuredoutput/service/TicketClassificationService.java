package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.ClassifyTicketRequest;
import com.example.structuredoutput.dto.TicketCategory;
import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.dto.TicketPriority;
import com.example.structuredoutput.model.ModelClient;
import com.example.structuredoutput.prompt.TicketPromptBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** 工单输入 -> 模型文本 -> JSON 解析 -> 字段校验 -> 有限重试或人工复核标记。 */
public class TicketClassificationService {
    private final ModelClient modelClient;
    private final ObjectMapper objectMapper;
    private final TicketClassificationValidator validator;
    private final int maxAttempts;
    private final StructuredOutputRunner runner = new StructuredOutputRunner();

    public TicketClassificationService(ModelClient modelClient, ObjectMapper objectMapper,
            TicketClassificationValidator validator, int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts 必须大于 0");
        this.modelClient = modelClient;
        // 固定本服务的解析规则，不修改调用方共享的 ObjectMapper。
        this.objectMapper = objectMapper.copy()
                // 不把字符串数字转换成数值，也不把整数当作枚举下标。
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
        this.maxAttempts = maxAttempts;
    }

    public TicketClassification classify(String ticketText) {
        // 普通方法不会自动触发 Bean Validation；输入校验要在重试之外显式执行。
        validator.validateRequest(new ClassifyTicketRequest(ticketText));
        // 每次分类独立保存修复提示，不能放到跨请求共享的成员变量里。
        AtomicReference<String> repairHint = new AtomicReference<>("");
        try {
            return runner.runWithRetry(() -> {
                try {
                    String prompt = TicketPromptBuilder.build(ticketText, repairHint.get());
                    String rawOutput = modelClient.generate(prompt);
                    TicketClassification result = objectMapper.readValue(rawOutput, TicketClassification.class);
                    validator.validate(result);
                    return result;
                } catch (JsonProcessingException ex) {
                    repairHint.set("上次不是符合契约的单个 JSON 对象。请检查字段名、枚举和类型，只输出 JSON。");
                    // 不保留原始解析异常，防止异常日志携带模型返回内容。
                    throw new IllegalArgumentException("模型 JSON 解析失败");
                } catch (TicketClassificationValidator.ValidationFailure ex) {
                    repairHint.set("上次字段校验失败：" + ex.getMessage() + "。请按契约重新输出 JSON。");
                    throw ex;
                } catch (RuntimeException ex) {
                    // 模型调用失败也进入有限重试，不把供应商响应或密钥拼进 Prompt。
                    repairHint.set("上次模型调用失败，请重新生成符合契约的 JSON。");
                    throw ex;
                }
            }, maxAttempts);
        } catch (StructuredOutputRunner.RetryExhaustedException ex) {
            System.out.println("结构化输出耗尽 " + maxAttempts + " 次尝试，返回人工复核标记");
            return fallback();
        }
    }

    private TicketClassification fallback() {
        // 这里只返回复核标记，不代表已经投递人工队列、落库或发送通知。
        // confidence=0.0 表示本次失败兜底，不是模型经过校准的置信度。
        return new TicketClassification(TicketCategory.OTHER, TicketPriority.MEDIUM,
                "模型分类失败，转人工处理", List.of("人工复核", "原始工单内容"), 0.0);
    }
}
