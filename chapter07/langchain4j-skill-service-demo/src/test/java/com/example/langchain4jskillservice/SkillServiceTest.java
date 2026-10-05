package com.example.langchain4jskillservice;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkillServiceTest {
    @Test void nativeSkillActivationAndConversationIsolation() {
        var model = new LangChain4jSkillServiceDemo.OfflineModel();
        var assistant = LangChain4jSkillServiceDemo.create(model);
        assertTrue(assistant.chat("a:u:c", "评审订单接口，内部标识 secret-order-a").contains("activate_skill"));
        assertEquals(2, model.requests.size(), "AI Services 应执行工具后再次调用模型");
        assertTrue(model.requests.get(1).messages().stream().anyMatch(m ->
                m instanceof ToolExecutionResultMessage t && t.text().contains("先确认接口契约")));
        assistant.chat("b:u:c", "评审另一个接口");
        assertEquals(4, model.requests.size(), "新会话应重新激活，不能沿用租户 A 的消息");
        assertFalse(model.requests.get(2).messages().toString().contains("secret-order-a"));
        assistant.chat("a:u:c", "继续检查异常处理");
        assertEquals(5, model.requests.size(), "同会话保留已激活 Skill 及历史");
        assertTrue(model.requests.get(4).messages().toString().contains("secret-order-a"));
    }
}
