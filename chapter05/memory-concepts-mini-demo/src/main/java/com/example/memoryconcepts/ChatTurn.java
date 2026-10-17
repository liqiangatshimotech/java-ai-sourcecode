package com.example.memoryconcepts;
import java.time.Instant;
/** 记录真实发生的对话，不因模型窗口裁剪而丢弃。 */
public record ChatTurn(String role,String content,Instant createdAt) {
    public static ChatTurn user(String content) { return new ChatTurn("user",content,Instant.now()); }
    public static ChatTurn assistant(String content) { return new ChatTurn("assistant",content,Instant.now()); }
}
