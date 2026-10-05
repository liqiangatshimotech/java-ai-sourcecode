package com.example.langchain4jticketclassifier.demo;

import com.example.langchain4jticketclassifier.assistant.ObservableTicketClassifierAssistant;
import com.example.langchain4jticketclassifier.assistant.TicketClassifierAssistant;
import com.example.langchain4jticketclassifier.dto.TicketClassification;
import com.example.langchain4jticketclassifier.service.TicketClassificationValidator;
import com.example.langchain4jticketclassifier.service.TicketClassifierFactory;
import com.example.langchain4jticketclassifier.service.TicketWorkflow;
import dev.langchain4j.service.Result;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class TicketClassifierDemo {

    public static void main(String[] args) {
        String baseUrl = env("OLLAMA_BASE_URL", "http://localhost:11434");
        String modelName = env("OLLAMA_MODEL", "qwen2.5:7b");
        String ticketText = args.length == 0
            ? "昨天扣费两次，发票也开不出来，客户经理一直没回复。"
            : String.join(" ", args);

        TicketClassifierFactory factory = new TicketClassifierFactory();
        TicketClassifierAssistant assistant = factory.createFromEnvironment();

        Validator jakartaValidator =
            Validation.buildDefaultValidatorFactory().getValidator();
        TicketWorkflow workflow = new TicketWorkflow(
            assistant,
            new TicketClassificationValidator(jakartaValidator)
        );

        TicketClassification classification = workflow.classifyAndRoute(ticketText);
        System.out.println(classification);

        ObservableTicketClassifierAssistant observable =
            factory.createObservableFromEnvironment();
        Result<TicketClassification> result =
            observable.classifyWithMetadata(ticketText);

        System.out.println("tokenUsage = " + result.tokenUsage());
        System.out.println("finishReason = " + result.finishReason());
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
