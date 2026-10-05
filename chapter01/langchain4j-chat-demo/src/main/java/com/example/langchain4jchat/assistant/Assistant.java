package com.example.langchain4jchat.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface Assistant {

    @SystemMessage("""
            你是 Java AI 应用开发实战课的助教。
            回答要面向 Java 后端工程师，先给结论，再给关键实现点。
            """)
    @UserMessage("{{message}}")
    String chat(@V("message") String message);
}
