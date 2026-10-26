package com.example.agentruntime.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
/** userId 是演示会话标识，生产应从认证上下文取得；orderNo 不再从任意文本猜测。 */
public record AgentRunRequest(
        @NotBlank @JsonAlias("sessionId") String userId,
        @NotBlank @JsonAlias("message") String goal,
        @NotBlank String orderNo,
        boolean allowWrite) {}
