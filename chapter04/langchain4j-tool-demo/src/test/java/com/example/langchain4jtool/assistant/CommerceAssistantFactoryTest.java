package com.example.langchain4jtool.assistant;

import com.example.langchain4jtool.service.CrmService;
import com.example.langchain4jtool.service.InventoryService;
import com.example.langchain4jtool.service.OrderService;
import com.example.langchain4jtool.service.ToolAuditLog;
import com.example.langchain4jtool.tool.CommerceTools;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommerceAssistantFactoryTest {

    @Test
    void aiServiceSendsToolSpecificationsToChatModel() {
        RecordingChatModel chatModel = new RecordingChatModel();
        CommerceTools tools = new CommerceTools(new OrderService(), new InventoryService(), new CrmService(), new ToolAuditLog());

        CommerceAssistant assistant = CommerceAssistantFactory.create(chatModel, tools);
        String answer = assistant.answer("查一下订单 O-1001");

        assertThat(answer).isEqualTo("测试模型响应");
        assertThat(chatModel.lastRequest.toolSpecifications())
            .extracting(spec -> spec.name())
            .containsExactlyInAnyOrder("query_order_status", "query_inventory", "query_customer_profile");
    }

    private static final class RecordingChatModel implements ChatModel {

        private ChatRequest lastRequest;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            this.lastRequest = request;
            return ChatResponse.builder()
                .aiMessage(AiMessage.from("测试模型响应"))
                .build();
        }

        @Override
        public ModelProvider provider() {
            return ModelProvider.OTHER;
        }
    }
}
