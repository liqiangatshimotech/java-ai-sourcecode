package com.example.langgraph4j.aftersales.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 启动新工作流。threadId 留空时由服务端生成 UUID。 */
public record StartWorkflowRequest(
        @Pattern(regexp = "[A-Za-z0-9_-]{1,36}", message = "threadId 格式不正确")
        String threadId,
        @NotBlank(message = "message 不能为空")
        String message) {
}
