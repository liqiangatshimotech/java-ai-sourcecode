package com.example.structuredoutput.demo;

import com.example.structuredoutput.dto.TicketClassification;
import com.example.structuredoutput.model.FakeModelClient;
import com.example.structuredoutput.model.HttpModelClient;
import com.example.structuredoutput.model.ModelClient;
import com.example.structuredoutput.service.TicketClassificationService;
import com.example.structuredoutput.service.TicketClassificationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/** 与文章一致的单次分类入口，显式装配对象，不需要启动 Spring 容器。 */
public class TicketClassificationDemo {
    public static void main(String[] args) throws Exception {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--live"))) {
            throw new IllegalArgumentException("不传参数运行离线示例；--live 调用真实模型");
        }
        // 默认用假模型稳定复现第一次失败、第二次成功，不需要密钥。
        // 只有显式传 --live 才发网络请求，真实供应商默认 DeepSeek。
        ModelClient modelClient = args.length == 0 ? new FakeModelClient() : HttpModelClient.fromEnvironment();
        ObjectMapper objectMapper = new ObjectMapper();
        // ValidatorFactory 持有校验资源；Validator 必须在工厂关闭之前使用。
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            TicketClassificationValidator validator = new TicketClassificationValidator(factory.getValidator());
            TicketClassificationService service = new TicketClassificationService(modelClient, objectMapper, validator, 3);
            TicketClassification result = service.classify("昨天扣费两次，发票也开不出来，客户经理一直没回复。");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        }
    }
}
