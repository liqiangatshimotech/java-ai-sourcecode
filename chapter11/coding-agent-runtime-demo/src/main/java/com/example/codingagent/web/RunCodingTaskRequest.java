package com.example.codingagent.web;

import jakarta.validation.constraints.NotBlank;

/** 客户端必须明确执行/修复哪份提案，重试不会误执行新版本。 */
public record RunCodingTaskRequest(@NotBlank String proposalId) { }
