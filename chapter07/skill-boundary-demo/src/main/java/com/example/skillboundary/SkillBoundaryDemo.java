package com.example.skillboundary;

import java.util.Map;

public class SkillBoundaryDemo {

    public static void main(String[] args) {
        TaskPrompt prompt = new TaskPrompt("审查这次订单接口改动");
        SkillGuide skill = new SkillGuide(
                "code-review",
                "任务涉及代码审查或 PR review 时使用",
                "先看变更，再看测试，最后按严重程度输出问题。",
                "问题优先，少写泛泛总结。"
        );

        ToolFunction diffTool = arguments -> new ToolResult(true, "读取到 3 个 Java 文件变更");
        ToolResult result = diffTool.call(Map.of("base", "main", "head", "feature/order-api"));

        System.out.println(prompt.text());
        System.out.println(skill.workflow());
        System.out.println(result.message());
    }

    record TaskPrompt(String text) {
    }

    interface ToolFunction {
        ToolResult call(Map<String, Object> arguments);
    }

    record McpToolDescriptor(String name, String description, Map<String, Object> inputSchema) {
    }

    record SkillGuide(String name, String whenToUse, String workflow, String qualityBar) {
    }

    record ToolResult(boolean success, String message) {
    }
}
