package com.example.aicustomerservice.entity;

/**
 * 客服会话状态。
 *
 * 状态迁移由 ConversationService 统一校验，Controller 和 DAO 不能绕过业务规则直接改状态。
 */
public enum ConversationStatus {
    /** 新消息已经进入队列，尚未被客服领取。 */
    WAITING,
    /** 客服已经领取或接管，正在处理。 */
    IN_PROGRESS,
    /** 客服已经回复，正在等待客户补充信息或确认结果。 */
    WAITING_CUSTOMER,
    /** 正在等待仓库、财务等内部团队返回处理结果。 */
    WAITING_INTERNAL,
    /** 问题已经解决，会话只允许查看，不再允许继续写入。 */
    RESOLVED
}
