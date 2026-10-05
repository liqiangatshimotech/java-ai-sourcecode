package com.example.a2a.protocol;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 之间传递的一条消息。
 *
 * <p>Message 用来发起任务、补充输入或传递状态说明；真正的任务结果应该放在 Artifact 里。
 */
public record Message(
        String messageId,
        String contextId,
        String taskId,
        Role role,
        List<Part> parts,
        Map<String, Object> metadata) {

    public static Message userText(String text) {
        return new Message(
                UUID.randomUUID().toString(),
                null,
                null,
                Role.ROLE_USER,
                List.of(Part.text(text)),
                null);
    }

    public static Message agentText(String contextId, String taskId, String text) {
        return new Message(
                UUID.randomUUID().toString(),
                contextId,
                taskId,
                Role.ROLE_AGENT,
                List.of(Part.text(text)),
                null);
    }

    public String firstText() {
        if (parts == null) {
            return "";
        }
        return parts.stream()
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("");
    }
}
