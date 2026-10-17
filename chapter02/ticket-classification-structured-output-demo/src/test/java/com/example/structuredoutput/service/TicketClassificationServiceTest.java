package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.TicketCategory;
import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.model.AlwaysInvalidModelClient;
import com.example.structuredoutput.model.FakeModelClient;
import com.example.structuredoutput.model.ModelClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** 用真实 Jackson 和 Bean Validation 验证契约，只有外部模型使用可控替身。 */
class TicketClassificationServiceTest {
    private static final String VALID = """
            {"category":"BILLING","priority":"HIGH","summary":"账单问题",
             "requiredData":["订单号"],"confidence":0.86}
            """;
    private ValidatorFactory factory;
    private TicketClassificationValidator validator;

    @BeforeEach void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new TicketClassificationValidator(factory.getValidator());
    }

    @AfterEach void cleanup() { factory.close(); }

    private TicketClassificationService service(ModelClient client, int attempts) {
        return new TicketClassificationService(client, new ObjectMapper(), validator, attempts);
    }

    @Test void retriesWhenFirstModelOutputIsNotJson() {
        FakeModelClient model = new FakeModelClient();
        var result = service(model, 3).classify("昨天扣费两次");
        assertThat(model.callCount()).isEqualTo(2);
        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
        assertThat(result.confidence()).isEqualTo(0.86);
    }

    @Test void returnsExplicitFallbackWhenAllAttemptsFailValidation() {
        AlwaysInvalidModelClient model = new AlwaysInvalidModelClient();
        var result = service(model, 2).classify("账单问题");
        assertThat(model.callCount()).isEqualTo(2);
        assertThat(result.summary()).isEqualTo("模型分类失败，转人工处理");
        assertThat(result.requiredData()).contains("人工复核");
        assertThat(result.confidence()).isZero();
    }

    static Stream<String> malformedOutputs() throws Exception {
        // 每个必填字段都分别验证“省略”和“显式 null”，避免只测一个成功示例。
        List<String> inputs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (String field : List.of("category", "priority", "summary", "requiredData", "confidence")) {
            ObjectNode missing = (ObjectNode) mapper.readTree(VALID);
            missing.remove(field);
            inputs.add(missing.toString());
            ObjectNode nil = (ObjectNode) mapper.readTree(VALID);
            nil.putNull(field);
            inputs.add(nil.toString());
        }
        inputs.add(VALID.replace("BILLING", "INVALID"));
        inputs.add(VALID.replace("HIGH", "INVALID"));
        inputs.add(VALID.replace("\"BILLING\"", "0"));
        inputs.add(VALID.replace("\"HIGH\"", "2"));
        inputs.add(VALID.replace("0.86", "\"0.86\""));
        inputs.add(VALID.replace("0.86", "1.8"));
        inputs.add(VALID.replace("0.86", "-0.1"));
        inputs.add(VALID.replace("账单问题", " "));
        inputs.add(VALID.replace("账单问题", "字".repeat(81)));
        inputs.add(VALID.replace("订单号", " "));
        inputs.add(VALID.replace("[\"订单号\"]", "[null]"));
        inputs.add(VALID.replace("\"confidence\"", "\"extra\":true,\"confidence\""));
        inputs.add(VALID + "{}"); // 不允许合法对象后面再追加第二个 JSON 对象。
        inputs.add("null");
        inputs.add("这是自然语言");
        return inputs.stream();
    }

    @ParameterizedTest @MethodSource("malformedOutputs")
    void invalidOutputMustBeRepairedBeforeReturning(String bad) {
        AtomicInteger calls = new AtomicInteger();
        var result = service(prompt -> calls.incrementAndGet() == 1 ? bad : VALID, 2).classify("账单问题");
        assertThat(calls).hasValue(2);
        assertThat(result.confidence()).isEqualTo(0.86);
        assertThat(result.summary()).isEqualTo("账单问题");
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\n"})
    void invalidInputDoesNotCallModel(String input) {
        AtomicInteger calls = new AtomicInteger();
        var service = service(prompt -> { calls.incrementAndGet(); return VALID; }, 3);
        assertThatThrownBy(() -> service.classify(input)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).hasValue(0);
    }

    @Test void overlongInputDoesNotCallModel() {
        var service = service(prompt -> { throw new AssertionError("不应调用模型"); }, 3);
        assertThatThrownBy(() -> service.classify("字".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void modelRuntimeFailureCanRecover() {
        AtomicInteger calls = new AtomicInteger();
        var result = service(prompt -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("网络错误");
            return VALID;
        }, 3).classify("账单问题");
        assertThat(calls).hasValue(2);
        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
    }

    @Test void persistentModelFailureReturnsFallbackWithinBudget() {
        AtomicInteger calls = new AtomicInteger();
        var result = service(prompt -> { calls.incrementAndGet(); throw new IllegalStateException("失败"); }, 3)
                .classify("账单问题");
        assertThat(calls).hasValue(3);
        assertThat(result.requiredData()).contains("人工复核");
    }

    @Test void repairHintNamesInvalidFieldWithoutLeakingRejectedValue() {
        List<String> prompts = new ArrayList<>();
        service(prompt -> {
            prompts.add(prompt);
            return prompts.size() == 1 ? VALID.replace("0.86", "1.8") : VALID;
        }, 2).classify("账单问题");
        assertThat(prompts.get(1)).contains("confidence", "DecimalMax").doesNotContain("1.8");
    }

    @Test void zeroConfidenceAndEmptyRequiredDataAreValid() {
        var result = service(prompt -> VALID.replace("0.86", "0.0").replace("[\"订单号\"]", "[]"), 1)
                .classify("账单问题");
        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
        assertThat(result.requiredData()).isEmpty();
        assertThat(result.confidence()).isZero();
    }

    @Test void nonPositiveAttemptsFailFast() {
        assertThatThrownBy(() -> service(prompt -> VALID, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StructuredOutputRunner().runWithRetry(() -> "ok", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void runnerPreservesLastFailureOnExhaustion() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException original = new IllegalStateException("test failure");
        assertThatThrownBy(() -> new StructuredOutputRunner().runWithRetry(() -> {
            calls.incrementAndGet(); throw original;
        }, 2)).isInstanceOf(StructuredOutputRunner.RetryExhaustedException.class).hasCause(original);
        assertThat(calls).hasValue(2);
    }

    @Test void cancellationMustNotBeRetriedOrTurnedIntoBusinessFallback() {
        AtomicInteger calls = new AtomicInteger();
        try {
            assertThatThrownBy(() -> service(prompt -> {
                calls.incrementAndGet();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("cancelled");
            }, 3).classify("账单问题")).isInstanceOf(IllegalStateException.class).hasMessage("cancelled");
            assertThat(calls).hasValue(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // 清理测试线程，避免影响后续测试。
        }
    }
}
