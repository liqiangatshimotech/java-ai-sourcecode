package com.example.structuredoutput.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 模型返回的业务契约；能解析成对象不代表字段已通过校验。 */
public record TicketClassification(
    // 分类和优先级只接受枚举中的值，缺失或 null 都不允许。
    @NotNull TicketCategory category,
    @NotNull TicketPriority priority,
    @NotBlank @Size(max = 80) String summary,
    // 无需补充信息时传 []；不能省略字段，列表元素也不能是空白。
    @NotNull List<@NotBlank String> requiredData,
    // 使用包装类型，避免缺字段时被 double 悄悄补成合法的 0.0。
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence
) {
}
