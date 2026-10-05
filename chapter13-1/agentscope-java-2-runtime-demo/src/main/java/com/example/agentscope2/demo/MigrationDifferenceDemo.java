package com.example.agentscope2.demo;

/**
 * 打印 1.x 到 2.0 的迁移差异。
 *
 * <p>这个类不依赖模型，适合在文档第一篇里作为最小可运行例子。
 */
public class MigrationDifferenceDemo {

    public static void main(String[] args) {
        String[][] rows = {
            {"定位", "1.x 更像 Agent 开发框架", "2.0 更像生产化 Agent Runtime"},
            {"模型", "OpenAI/Ollama 常在 core 里直接使用", "RC5 起模型 provider 拆到 extensions-model-*"},
            {"状态", "Agent 容易承载会话状态", "Agent 无状态，调用时用 RuntimeContext 绑定 userId/sessionId"},
            {"扩展", "Hook 是主要切入点", "Middleware 成为主扩展面，Hook 进入兼容路径"},
            {"长任务", "Memory、Plan、Pipeline 分散组合", "HarnessAgent 统一 Workspace、Memory、Plan Mode、Subagent"},
            {"安全", "工具权限需要业务侧额外约束", "权限、HITL、Sandbox、Workspace 隔离进入框架层"}
        };

        for (String[] row : rows) {
            System.out.printf("%s：%s -> %s%n", row[0], row[1], row[2]);
        }
    }
}
