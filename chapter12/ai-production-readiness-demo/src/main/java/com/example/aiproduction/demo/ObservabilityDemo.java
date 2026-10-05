package com.example.aiproduction.demo;

import com.example.aiproduction.observability.AgentTrace;
import java.util.Map;

/**
 * 12.3 可观测性例子。
 */
public class ObservabilityDemo {

    public static void main(String[] args) {
        AgentTrace trace = new AgentTrace("trace-ticket-001");
        trace.add("request.received", "收到工单处理请求");
        trace.add("tool.called", "调用订单查询工具", Map.of("toolName", "order.query"));
        trace.add("model.completed", "模型生成结构化结果", Map.of("model", "deepseek-v4-flash"));

        trace.events().forEach(System.out::println);
    }
}
