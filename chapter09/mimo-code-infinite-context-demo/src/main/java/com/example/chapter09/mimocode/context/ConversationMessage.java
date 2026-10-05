package com.example.chapter09.mimocode.context;

/**
 * 一个进入上下文预算的消息块。真实 MiMo-Code 会从数据库里的 message/part
 * 结构转换成模型请求，这里用一个 record 表示最小信息：角色、内容、估算 token、
 * 工具名，以及这个工具结果是否可以在重建后被微压缩。
 */
public record ConversationMessage(
        String id,
        Role role,
        String text,
        int estimatedTokens,
        String toolName,
        boolean compactableToolResult,
        boolean compacted
) {
    public enum Role {
        USER,
        ASSISTANT,
        TOOL_RESULT
    }

    public static ConversationMessage user(String id, String text, int estimatedTokens) {
        return new ConversationMessage(id, Role.USER, text, estimatedTokens, "", false, false);
    }

    public static ConversationMessage assistant(String id, String text, int estimatedTokens) {
        return new ConversationMessage(id, Role.ASSISTANT, text, estimatedTokens, "", false, false);
    }

    public static ConversationMessage toolResult(String id, String toolName, String text, int estimatedTokens, boolean compactable) {
        return new ConversationMessage(id, Role.TOOL_RESULT, text, estimatedTokens, toolName, compactable, false);
    }

    public ConversationMessage compactedCopy() {
        if (!compactableToolResult || compacted) {
            return this;
        }
        return new ConversationMessage(
                id,
                role,
                "[old " + toolName + " result cleared; rerun the tool if the exact body is needed]",
                Math.min(estimatedTokens, 120),
                toolName,
                true,
                true);
    }

    public String shortLine() {
        String prefix = role == Role.TOOL_RESULT ? role + "(" + toolName + ")" : role.toString();
        return "- " + id + " " + prefix + " ~" + estimatedTokens + " tokens: " + text;
    }
}
