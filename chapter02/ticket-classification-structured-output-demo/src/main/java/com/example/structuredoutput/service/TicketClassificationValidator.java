package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.ClassifyTicketRequest;
import com.example.structuredoutput.dto.TicketClassification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Collectors;

/** 可以由 Spring 注入，也可以在 Main 中手工创建；两种方式都需要显式调用校验方法。 */
@Component
public class TicketClassificationValidator {
    private final Validator validator;

    public TicketClassificationValidator(Validator validator) {
        this.validator = validator;
    }

    public void validateRequest(ClassifyTicketRequest request) {
        // 输入错误来自调用方，应该直接拒绝，不应消耗模型重试次数。
        check(request);
    }

    public void validate(TicketClassification result) {
        check(result);
    }

    private <T> void check(T value) {
        if (value == null) throw new ValidationFailure("对象不能为 null");
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            // 只返回字段路径和约束名称，不带工单正文或模型返回的敏感值。
            String summary = violations.stream()
                    .map(v -> v.getPropertyPath() + " "
                            + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                    .sorted().collect(Collectors.joining("; "));
            throw new ValidationFailure(summary);
        }
    }

    /** 消息只包含程序生成的校验摘要，可以安全放入下一次模型请求。 */
    public static class ValidationFailure extends IllegalArgumentException {
        public ValidationFailure(String summary) { super(summary); }
    }
}
