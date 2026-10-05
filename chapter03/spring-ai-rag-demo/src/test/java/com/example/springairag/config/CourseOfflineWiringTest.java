package com.example.springairag.config;

import com.example.springairag.service.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ai.chat.client.ChatClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** fake 模式不创建真实模型 Builder，避免启动时无意义要求云模型凭据。 */
class CourseOfflineWiringTest {
    @Test void fakeModeOnlyLoadsFakeGateway() {
        new ApplicationContextRunner().withUserConfiguration(CourseChatModelConfiguration.class,
            FakeGroundedAiGateway.class,SpringAiGateway.class).withPropertyValues("app.rag.ai-mode=fake")
            .run(context->{
                assertNull(context.getStartupFailure());
                assertInstanceOf(FakeGroundedAiGateway.class,context.getBean(AiGateway.class));
                assertEquals(0,context.getBeansOfType(ChatClient.Builder.class).size());
            });
    }
}
