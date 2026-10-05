package com.example.langchain4jrag.demo;

import com.example.langchain4jrag.service.PolicyRagService;
import com.example.langchain4jrag.service.PolicyRagService.AskResult;

public class PolicyAssistantDemo {

    public static void main(String[] args) {
        String tenantId = args.length > 0 ? args[0] : "acme";
        String question = args.length > 1
            ? joinQuestion(args)
            : "企业版客户重复扣费怎么办？";

        PolicyRagService ragService = PolicyRagService.createFromEnvironment();
        System.out.println(Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL","false"))
            ? "REAL_MODEL："+com.example.langchain4jrag.config.CourseChatSettings.from(System.getenv())
            : "OFFLINE：GroundedChatModel 规则回答，未调用真实模型");
        AskResult result = ragService.ask(question, tenantId, 4);

        System.out.println("tenantId = " + tenantId);
        System.out.println("question = " + question);
        System.out.println();
        System.out.println(result.answer());
        System.out.println();
        System.out.println("sources:");
        result.sources().forEach(source -> System.out.printf(
            "- %s [%s] score=%.4f%n",
            source.title(),
            source.source(),
            source.score()
        ));
    }

    private static String joinQuestion(String[] args) {
        StringBuilder question = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (!question.isEmpty()) {
                question.append(' ');
            }
            question.append(args[i]);
        }
        return question.toString();
    }
}
