package com.example.agentruntime.domain;

import com.example.agentruntime.tool.ToolCall;
import java.util.*;
/** 可变状态只由 Runtime 在本次运行的锁内修改；对外只返回快照。 */
public final class AgentState {
    private final String runId, userId, goal, orderNo;
    private final List<AgentStepRecord> steps = new ArrayList<>();
    private final Map<String, String> facts = new LinkedHashMap<>();
    private String status = "RUNNING", finalAnswer = "";
    private ToolCall pendingCall;
    public AgentState(String runId, String userId, String goal, String orderNo) {
        this.runId = runId; this.userId = userId; this.goal = goal; this.orderNo = orderNo;
    }
    public String runId() { return runId; }
    public String userId() { return userId; }
    public String goal() { return goal; }
    public String orderNo() { return orderNo; }
    public String status() { return status; }
    public String finalAnswer() { return finalAnswer; }
    public boolean hasFact(String key) { return facts.containsKey(key); }
    public String fact(String key) { return facts.get(key); }
    public int nextStepNo() { return steps.size() + 1; }
    public Optional<String> pendingApproval() { return Optional.ofNullable(facts.get("pendingApproval")); }
    public ToolCall pendingCall() { return pendingCall; }
    public void record(AgentStepRecord step, Map<String, String> newFacts) {
        steps.add(step); facts.putAll(newFacts);
    }
    public void pause(ToolCall call, String reason) {
        pendingCall = call; facts.put("pendingApproval", reason); status = "WAITING_APPROVAL";
    }
    public void resume() {
        pendingCall = null; facts.remove("pendingApproval"); status = "RUNNING";
    }
    public void finish(String status, String answer) { this.status = status; this.finalAnswer = answer; }
    public AgentRunTrace snapshot() {
        return new AgentRunTrace(runId, userId, goal, orderNo, status, finalAnswer, steps, facts);
    }
}
