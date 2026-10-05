package com.example.agentscopeframework.project;

import java.util.List;

/**
 * 工单处理结果。
 *
 * <p>这个类既可以作为结构化输出 schema，也可以作为后端接口 DTO。字段使用 public 是为了兼容大模型结构化输出反序列化，
 * 同时保留无参构造方法。
 */
public class TicketDecision {
    public String category;
    public TicketPriority priority;
    public String summary;
    public String replyDraft;
    public boolean needHumanReview;
    public List<String> toolEvidence;
    public List<String> nextActions;

    public TicketDecision() {}

    public TicketDecision(
            String category,
            TicketPriority priority,
            String summary,
            String replyDraft,
            boolean needHumanReview,
            List<String> toolEvidence,
            List<String> nextActions) {
        this.category = category;
        this.priority = priority;
        this.summary = summary;
        this.replyDraft = replyDraft;
        this.needHumanReview = needHumanReview;
        this.toolEvidence = toolEvidence;
        this.nextActions = nextActions;
    }
}
