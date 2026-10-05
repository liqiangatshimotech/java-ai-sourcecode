package com.example.langgraph4j.aftersales.ai;

import com.example.langgraph4j.aftersales.domain.IntentAnalysis;
import com.example.langgraph4j.aftersales.domain.OrderSnapshot;

/**
 * 业务图使用的模型能力边界。
 *
 * <p>节点只认识这个接口，不认识 DeepSeekChatModel、OllamaChatModel 或
 * OpenAiChatModel。测试因此可以换成确定性实现，生产环境也能无痛切模型。</p>
 */
public interface AfterSalesAssistant {

    IntentAnalysis analyzeRequest(String userMessage, String knownOrderId);

    String answerConsultation(String userMessage, OrderSnapshot order);

    String explainRefundResult(String decision, OrderSnapshot order, String detail);
}
