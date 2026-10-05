package com.example.aiproduction.security;

import java.util.Set;

/**
 * 工具风险策略。
 *
 * <p>只读工具可以自动执行，写操作和破坏性操作必须审批。这里用工具名模拟真实系统里的权限矩阵。
 */
public class ToolRiskPolicy {

    private final Set<String> highRiskTools =
            Set.of("refund.create", "user.delete", "database.drop", "deployment.rollback");

    public boolean requiresApproval(String toolName) {
        return highRiskTools.contains(toolName);
    }
}
