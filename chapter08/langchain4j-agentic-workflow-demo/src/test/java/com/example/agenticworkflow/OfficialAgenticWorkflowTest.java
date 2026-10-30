package com.example.agenticworkflow;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OfficialAgenticWorkflowTest {
    @Test void renderedPromptCarriesFactsQuestionAndLatestReviewFeedback() {
        // 捕获真正进入 ChatModel 的请求，验证注解模板经过官方代理解析，而不是只检查源码文本。
        var prompts = new java.util.ArrayList<String>();
        ChatModel recording = new ChatModel() {
            public ChatResponse doChat(ChatRequest request) {
                String userText = request.messages().stream()
                        .filter(dev.langchain4j.data.message.UserMessage.class::isInstance)
                        .map(dev.langchain4j.data.message.UserMessage.class::cast)
                        .map(dev.langchain4j.data.message.UserMessage::singleText)
                        .reduce((first, last) -> last).orElseThrow();
                prompts.add(userText);
                // 第一轮不合格，第二轮合格，确保实际经历反馈回灌。
                return ChatResponse.builder().aiMessage(AiMessage.from(prompts.size() == 1
                        ? "A100 配送延误，建议评估补偿。"
                        : "A100 配送延误 2 天，补偿发放前必须人工确认。")).build();
            }
        };
        OfficialAgenticWorkflowDemo.run(recording, "A100", "我的订单为什么还没有到", 2);
        assertEquals(2, prompts.size());
        for (String prompt : prompts) {
            assertTrue(prompt.contains("你是订单售后助手"));
            assertTrue(prompt.contains("A100，已支付，金额 199 元"));
            assertTrue(prompt.contains("A100，配送延误 2 天"));
            assertTrue(prompt.contains("我的订单为什么还没有到"));
            assertTrue(prompt.contains("只提出补偿建议，不宣称已发放"));
            assertFalse(prompt.contains("{{"), "模板变量必须全部解析后再交给模型");
        }
        assertTrue(prompts.get(0).contains("首次生成，请核对人工确认条件"));
        assertTrue(prompts.get(1).contains("请补全订单 A100 与人工确认条件，不要承诺已发放或已到账。"));
        assertFalse(prompts.get(1).contains("首次生成，请核对人工确认条件"));
    }

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
