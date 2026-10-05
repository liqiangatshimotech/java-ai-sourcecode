package com.example.codingagent.runtime;

import com.example.codingagent.tool.ToolRisk;

/**
 * 工具风险策略：读操作直接放行，写操作和外部副作用必须先审批。
 */
public final class ToolRiskPolicy {

    public boolean requiresApproval(ToolRisk risk) {
        return risk == ToolRisk.HIGH;
    }
}
