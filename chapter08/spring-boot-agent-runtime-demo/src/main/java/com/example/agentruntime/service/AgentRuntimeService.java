package com.example.agentruntime.service;

import com.example.agentruntime.business.*;
import com.example.agentruntime.config.AgentProperties;
import com.example.agentruntime.domain.*;
import com.example.agentruntime.event.*;
import com.example.agentruntime.planner.*;
import com.example.agentruntime.repository.*;
import com.example.agentruntime.tool.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 只做编排：规划、权限、执行、记录。业务判断和事件存储各自有独立文件。 */
@Service
public class AgentRuntimeService {
    private final AgentProperties properties;
    private final AgentPlanner planner;
    private final ToolRegistry registry;
    private final ToolPermissionGuard guard = new ToolPermissionGuard();
    private final StopCondition stopCondition;
    private final AgentTraceRepository traces;
    private final AgentEventPublisher events;
    private final Map<String, AgentState> states = new ConcurrentHashMap<>();
    private final Map<String, List<String>> sessions = new ConcurrentHashMap<>();

    @Autowired // 多个构造器时显式指定 Spring 的注入入口，单测仍可传替身。
    public AgentRuntimeService(AgentProperties properties) {
        this(properties, new RulePlanner(), demoTools(),
                new InMemoryAgentTraceRepository(), new InMemoryAgentEventPublisher());
    }
    private static ToolRegistry demoTools() {
        DemoOrderData data = new DemoOrderData();
        return ToolRegistry.afterSales(new AfterSalesTools(data, data));
    }
    public AgentRuntimeService(AgentProperties properties, AgentPlanner planner, ToolRegistry registry,
            AgentTraceRepository traces, AgentEventPublisher events) {
        this.properties = properties; this.planner = planner; this.registry = registry;
        this.traces = traces; this.events = events;
        this.stopCondition = new StopCondition(properties.runtime().maxSteps());
    }
    public AgentRunResponse run(AgentRunRequest request) {
        if (request == null || blank(request.userId()) || blank(request.goal()) || blank(request.orderNo())) {
            throw new IllegalArgumentException("userId、goal 和 orderNo 不能为空");
        }
        AgentState state = new AgentState(UUID.randomUUID().toString(), request.userId(),
                request.goal(), request.orderNo().trim());
        states.put(state.runId(), state);
        appendHistory(state.userId(), "用户：" + request.goal());
        synchronized (state) {
            traces.save(state.snapshot());
            events.publish(state.runId(), "RUN_STARTED", "开始处理订单 " + state.orderNo());
            return advance(state, request.allowWrite());
        }
    }
    public AgentRunResponse approve(String runId) {
        AgentState state = requiredState(runId);
        synchronized (state) {
            if (!"WAITING_APPROVAL".equals(state.status()) || state.pendingCall() == null)
                throw new IllegalStateException("当前运行没有等待确认的写操作");
            if (stopCondition.shouldStop(state)) return finish(state, "STOPPED", "达到最大执行步数，未执行挂起动作。");
            // 先取冻结的原动作，绝不重新规划出一个不同动作后沿用本次授权。
            ToolCall approved = state.pendingCall();
            state.resume();
            events.publish(runId, "APPROVED", "人工确认：" + approved.toolName());
            if (!execute(state, AgentDecision.callTool("执行已确认的挂起动作", approved), true)) return response(state);
            // 授权只消费一次。后续新的写工具仍需重新确认。
            return advance(state, false);
        }
    }
    private AgentRunResponse advance(AgentState state, boolean allowWrite) {
        while (true) {
            AgentDecision decision;
            try { decision = Objects.requireNonNull(planner.next(state), "Planner 返回空决策"); }
            catch (RuntimeException e) { return finish(state, "FAILED", "规划失败：" + e.getMessage()); }
            // 最后一个工具完成后仍允许 Finish，避免恰好用尽额度时误报 STOPPED。
            if (decision.finished()) return finish(state, "COMPLETED", decision.answer());
            if (stopCondition.shouldStop(state)) return finish(state, "STOPPED", "达到最大执行步数，运行已停止。");
            if (!execute(state, decision, allowWrite)) return response(state);
        }
    }
    private boolean execute(AgentState state, AgentDecision decision, boolean allowWrite) {
        ToolCall call = decision.call();
        ToolObservation observation;
        try {
            ToolDefinition tool = registry.get(call.toolName());
            ToolAuthorization authorization = guard.authorize(new AgentRunRequest(
                    state.userId(), state.goal(), state.orderNo(), allowWrite), tool);
            observation = authorization.allowed() ? registry.call(call)
                    : ToolObservation.blocked(authorization.reason(), Map.of());
        } catch (RuntimeException e) {
            // 工具名不在白名单也写入失败轨迹，不把异常抛穿后遗留 RUNNING。
            observation = ToolObservation.failed(e.getMessage());
        }
        state.record(new AgentStepRecord(state.nextStepNo(), decision.thought(), call.toolName(),
                call.arguments(), observation.status(), observation.content(), Instant.now()), observation.facts());
        traces.save(state.snapshot());
        events.publish(state.runId(), "TOOL_" + observation.status(), call.toolName() + "：" + observation.content());
        if ("BLOCKED".equals(observation.status())) {
            state.pause(call, observation.content());
            finish(state, "WAITING_APPROVAL", "售后判断已完成，等待确认工具 " + call.toolName() + "；尚未执行写操作。");
            return false;
        }
        if ("FAILED".equals(observation.status())) {
            finish(state, "FAILED", "工具失败，已停止；请核对轨迹：" + observation.content());
            return false;
        }
        return true;
    }
    private AgentRunResponse finish(AgentState state, String status, String answer) {
        state.finish(status, answer);
        traces.save(state.snapshot());
        events.publish(state.runId(), status, answer);
        appendHistory(state.userId(), "助手：" + answer);
        return response(state);
    }
    private AgentRunResponse response(AgentState state) {
        AgentRunTrace trace = state.snapshot();
        return new AgentRunResponse(state.runId(), state.userId(), properties.model().provider(),
                state.status(), state.finalAnswer(), state.pendingApproval().orElse(""), trace.steps(), trace.facts());
    }
    private void appendHistory(String userId, String message) {
        List<String> history = sessions.computeIfAbsent(userId, ignored -> new ArrayList<>());
        synchronized (history) {
            history.add(message);
            while (history.size() > properties.runtime().sessionHistoryLimit()) history.removeFirst();
        }
    }
    public AgentRunTrace trace(String runId) {
        return traces.findByRunId(runId).orElseThrow(() -> new IllegalArgumentException("找不到运行：" + runId));
    }
    public List<AgentEvent> events(String runId) { requiredState(runId); return events.findByRunId(runId); }
    public SseEmitter subscribe(String runId) { requiredState(runId); return events.subscribe(runId); }
    public List<String> history(String userId) {
        List<String> history = sessions.get(userId);
        if (history == null) return List.of();
        synchronized (history) { return List.copyOf(history); }
    }
    private AgentState requiredState(String runId) {
        AgentState state = states.get(runId);
        if (state == null) throw new IllegalArgumentException("找不到运行：" + runId);
        return state;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
