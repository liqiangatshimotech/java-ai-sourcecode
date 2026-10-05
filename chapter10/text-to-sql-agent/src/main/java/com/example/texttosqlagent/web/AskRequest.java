package com.example.texttosqlagent.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 提问请求体。
 *
 * @param question 自然语言问题，@NotBlank 保证空问题在 Controller 入口就被 400 拦下
 */
public record AskRequest(@NotBlank(message = "问题不能为空") String question) {
}
