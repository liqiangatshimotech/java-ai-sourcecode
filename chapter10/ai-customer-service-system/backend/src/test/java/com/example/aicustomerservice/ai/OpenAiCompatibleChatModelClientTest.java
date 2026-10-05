package com.example.aicustomerservice.ai;

import com.example.aicustomerservice.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatModelClientTest {

    @Test
    void shouldRejectDeepSeekWithoutApiKey() {
        ModelSettings settings = new ModelSettings(
            AiProvider.DEEPSEEK,
            "https://api.deepseek.com/v1",
            null,
            "deepseek-v4-flash",
            0.2
        );

        OpenAiCompatibleChatModelClient client = new OpenAiCompatibleChatModelClient(settings);

        assertThatThrownBy(client::validateConfiguration)
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("DEEPSEEK_API_KEY");
    }
}
