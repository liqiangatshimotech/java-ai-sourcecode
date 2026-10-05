package com.example.aiproduction.cost;

/**
 * 简化版模型路由器。
 *
 * <p>生产项目里通常会综合任务类型、上下文长度、预算、延迟、供应商可用性和用户等级。这个示例先保留最容易讲清楚的三条规则：
 * 本地开发走 Ollama，复杂任务走强模型，普通任务走小模型。
 */
public class ModelRouter {

    public ModelRouteDecision route(String taskType, TokenUsage usage, boolean localOnly) {
        if (localOnly) {
            return new ModelRouteDecision(ModelProfile.localOllama(), "本地验证任务，不请求远程模型");
        }

        boolean complexTask =
                taskType.contains("代码")
                        || taskType.contains("推理")
                        || taskType.contains("排障")
                        || usage.totalTokens() > 8_000;
        if (complexTask) {
            return new ModelRouteDecision(ModelProfile.strongDeepSeek(), "任务复杂或上下文较长，使用强模型");
        }

        return new ModelRouteDecision(ModelProfile.smallDeepSeek(), "普通任务优先使用低成本模型");
    }
}
