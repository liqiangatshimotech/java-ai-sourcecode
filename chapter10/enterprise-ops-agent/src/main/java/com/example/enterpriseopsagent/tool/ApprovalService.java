package com.example.enterpriseopsagent.tool;

/**
 * 审批服务。
 *
 * 回滚、重启、扩容这类动作属于高风险操作。Agent 只能创建审批单，
 * 不能直接执行生产变更。
 */
public interface ApprovalService {
    String createRollbackApproval(String serviceName, String environment, String reason);
}
