package com.example.langgraph4j.aftersales.web;

import jakarta.validation.constraints.NotBlank;

/** 在 wait_for_information 断点补充的新消息。 */
public record InformationRequest(@NotBlank(message = "message 不能为空") String message) {
}
