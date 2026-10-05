package com.example.agentscope2.bailian.project;

/**
 * 结构化输出示例对象。
 *
 * <p>这里用一个很小的业务对象承接模型输出，重点是展示百炼模型接入 AgentScope 后，
 * 仍然可以沿用 AgentScope 2.0 的结构化输出能力。
 */
public record BailianRoutingAdvice(
        String provider,
        String model,
        String recommendedScenario,
        String risk,
        String nextAction) {}
