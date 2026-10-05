package com.example.agenticworkflow;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OfficialAgenticWorkflowTest {
    @Test void officialSequenceParallelLoopAndScopeWorkTogether() {
        var model = new OfficialAgenticWorkflowDemo.OfflineModel();
        var result = OfficialAgenticWorkflowDemo.run(model,"A100","查询售后",2);
        assertEquals(2,model.calls.get(),"第一次质量不达标必须重新生成");
        assertTrue(result.reply().contains("人工确认"));
        assertTrue(result.scope().state().keySet().containsAll(java.util.Set.of("order","logistics","reply","feedback")));
        assertEquals("PASS",result.scope().readState("feedback"));
        assertTrue(result.scope().agentInvocations().size() >= 6);
    }
    @Test void exhaustedLoopDoesNotReturnUnapprovedReply() {
        ChatModel bad = new ChatModel() {
            public ChatResponse doChat(ChatRequest request) {
                return ChatResponse.builder().aiMessage(AiMessage.from("补偿已到账")).build();
            }
        };
        assertThrows(IllegalStateException.class, () -> OfficialAgenticWorkflowDemo.run(bad,"A100","查询售后",2));
    }
    @Test void unknownOrderFailsBeforeModelInvocation() {
        var model = new OfficialAgenticWorkflowDemo.OfflineModel();
        assertThrows(Exception.class, () -> OfficialAgenticWorkflowDemo.run(model,"OTHER","查询售后",2));
        assertEquals(0,model.calls.get());
    }
}
