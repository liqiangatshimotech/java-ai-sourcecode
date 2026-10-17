package com.example.memoryservice.memory;
import java.util.Map;
import org.springframework.stereotype.Component;
/** 保存前再次限定可进入模型上下文的内容，不允许任意原文进入长期记录。 */
@Component
public class MemoryValidator {
    private static final Map<String,String> ALLOWED=Map.of("language","回答用中文","style","回答保持简短",
            "response","回答用中文，保持简短","profile","用户自述负责华东区售后");
    public void validate(MemoryDecision decision) {
        if(!decision.shouldRemember()||!decision.normalizedContent().equals(ALLOWED.get(decision.memoryType())))
            throw new IllegalArgumentException("未经确认的记忆内容");
    }
}
