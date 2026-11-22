package com.example.aicustomerservice.entity;

/**
 * 一次业务工具调用的可追溯摘要。
 *
 * summary 是给模型提示、接口响应和 AI Run 看的文字；requiresHumanReview 是给 Java 护栏看的结构化结论，
 * 表示这次工具结果必须由人工确认（目前只有退款工具会置为 true）。
 */
public record ToolTrace(
    String toolName,
    String summary,
    boolean requiresHumanReview
) {
    /** 订单、物流这类只读查询不需要人工确认，沿用两个参数的写法。 */
    public ToolTrace(String toolName, String summary) {
        this(toolName, summary, false);
    }
}
