package com.example.structuredoutput.demo;

import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.model.AlwaysInvalidModelClient;
import com.example.structuredoutput.model.FakeModelClient;
import com.example.structuredoutput.service.DemoFactory;
import com.example.structuredoutput.service.TicketClassificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TicketClassificationDemo {

    private static final String TICKET_TEXT =
        "昨天扣费两次，发票也开不出来，客户经理一直没回复。";

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = DemoFactory.createObjectMapper();

        runRetrySuccessDemo(objectMapper);
        System.out.println();
        runFallbackDemo(objectMapper);
    }

    private static void runRetrySuccessDemo(ObjectMapper objectMapper) throws Exception {
        System.out.println("=== Demo 1: 第一次失败，第二次重试成功 ===");

        FakeModelClient modelClient = new FakeModelClient();
        TicketClassificationService service =
            DemoFactory.createService(modelClient, 3);

        TicketClassification result = service.classify(TICKET_TEXT);

        System.out.println("模型调用次数：" + modelClient.callCount());
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    private static void runFallbackDemo(ObjectMapper objectMapper) throws Exception {
        System.out.println("=== Demo 2: 多次失败后进入兜底 ===");

        AlwaysInvalidModelClient modelClient = new AlwaysInvalidModelClient();
        TicketClassificationService service =
            DemoFactory.createService(modelClient, 2);

        TicketClassification result = service.classify(TICKET_TEXT);

        System.out.println("模型调用次数：" + modelClient.callCount());
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}
