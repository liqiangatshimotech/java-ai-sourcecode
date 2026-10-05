package com.example.springaiskilladvisor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkillAdvisorTest {
    @Test void injectsSkillThroughRealAdvisorWithoutLosingBaseInstructions() {
        var model = new SpringAiSkillAdvisorDemo.OfflineModel();
        var answer = SpringAiSkillAdvisorDemo.client(model).prompt().user("请评审订单接口").call().content();
        assertTrue(answer.contains("offline"));
        assertTrue(model.lastPrompt().getSystemMessage().getText().contains("spring-boot-api-review"));
        assertTrue(model.lastPrompt().getSystemMessage().getText().contains("缺少 diff"));
        assertEquals("请评审订单接口", model.lastPrompt().getUserMessage().getText());
    }
    @Test void unrelatedTaskPassesWithoutSkill() {
        var model = new SpringAiSkillAdvisorDemo.OfflineModel();
        SpringAiSkillAdvisorDemo.client(model).prompt().user("解释 Java 集合").call().content();
        assertFalse(model.lastPrompt().getSystemMessage().getText().contains("spring-boot-api-review"));
    }
}
