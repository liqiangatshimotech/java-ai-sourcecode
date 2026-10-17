package com.example.memoryconcepts;
/** 示例中的业务状态快照；真实值应来自工单系统，不能由对话中的自述直接修改。 */
public record SupportSessionState(String userId,String region,String activeTicketId) {}
