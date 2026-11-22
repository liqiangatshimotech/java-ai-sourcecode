package com.example.aicustomerservice.ai;

import com.example.aicustomerservice.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatModelClientTest {

    @Test
    void shouldRejectDeepSeekWithoutApiKey() {
        // 显式构造缺少 API Key 的 DeepSeek 配置，避免测试依赖本机环境变量。
        ModelSettings settings = new ModelSettings(
            AiProvider.DEEPSEEK,
            "https://api.deepseek.com/v1",
            null,
            "deepseek-v4-flash",
            0.2
        );
        OpenAiCompatibleChatModelClient client =
            new OpenAiCompatibleChatModelClient(settings);

        // 配置错误在真正发送 HTTP 请求前暴露，并给出缺失变量名称。
        assertThatThrownBy(client::validateConfiguration)
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("DEEPSEEK_API_KEY");
    }
}
