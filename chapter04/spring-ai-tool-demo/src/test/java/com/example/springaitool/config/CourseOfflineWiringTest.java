package com.example.springaitool.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 不依赖当前机器的密钥；默认构建可用 ChatClient，但回答必须明确离线。 */
class CourseOfflineWiringTest {
    @Test void defaultBuilderIsUsableAndExplicitlyOffline() {
        String answer=CourseChatModelConfiguration.createBuilder(Map.of()).build().prompt("hello").call().content();
        assertTrue(answer.startsWith("OFFLINE"));
    }
}
