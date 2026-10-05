package com.example.langgraph4j.aftersales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 工作流自身的双层循环保护配置。 */
@ConfigurationProperties(prefix = "aftersales.workflow")
public class WorkflowProperties {
    /** 业务上最多允许补充信息的次数。 */
    private int maxClarificationRounds = 3;
    /** LangGraph4j 全图递归上限，防止错误边配置造成无限循环。 */
    private int recursionLimit = 40;

    public int getMaxClarificationRounds() { return maxClarificationRounds; }
    public void setMaxClarificationRounds(int value) { this.maxClarificationRounds = value; }
    public int getRecursionLimit() { return recursionLimit; }
    public void setRecursionLimit(int recursionLimit) { this.recursionLimit = recursionLimit; }
}
