package com.example.agentruntime.service;

import com.example.agentruntime.config.AgentProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 订单售后 Agent Runtime。Planner 决定下一步，Registry 执行工具，Guard 负责权限；
 * 每一步都留下轨迹和事件。默认 Planner 使用确定性规则，离线即可演示完整业务链路。
 */
@Service
public class AgentRuntimeService {
    private final AgentProperties properties;
    private final Planner planner;
    private final ToolRegistry registry;
    private final ToolPermissionGuard guard = new ToolPermissionGuard();
    private final AgentTraceRepository traces;
    private final AgentEventPublisher events;
    private final Map<String, AgentState> states = new ConcurrentHashMap<>();
    private final Map<String, List<String>> sessions = new ConcurrentHashMap<>();

    public AgentRuntimeService(AgentProperties properties) {
        this(properties, new RulePlanner(), ToolRegistry.afterSales(new AfterSalesTools()),
                new InMemoryAgentTraceRepository(), new InMemoryAgentEventPublisher());
    }

    /** 可注入依赖，测试时无需启动 Web 容器或调用模型。 */
    public AgentRuntimeService(AgentProperties properties, Planner planner, ToolRegistry registry,
                               AgentTraceRepository traces, AgentEventPublisher events) {
        this.properties = properties;
        this.planner = planner;
        this.registry = registry;
        this.traces = traces;
        this.events = events;
    }

    public synchronized RunResponse run(AgentRunRequest request) {
        if (request.sessionId() == null || request.sessionId().isBlank()
                || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("sessionId 和 message 不能为空");
        }
        String orderNo = extractOrderNo(request.message());
        AgentState state = new AgentState(UUID.randomUUID().toString(), request.sessionId(),
                request.message(), orderNo);
        states.put(state.runId, state);
        sessions.computeIfAbsent(request.sessionId(), ignored -> new ArrayList<>()).add("用户：" + request.message());
        events.publish(state.runId, "RUN_STARTED", "开始处理订单 " + orderNo);
        return advance(state, request.allowWrite());
    }

    /** 人工确认只针对先前确实被拦截的工具；状态内保存调用参数，客户端不能替换。 */
    public synchronized RunResponse approve(String runId) {
        AgentState state = requiredState(runId);
        if (!"WAITING_APPROVAL".equals(state.status) || state.pendingCall == null) {
            throw new IllegalStateException("当前运行没有等待确认的写操作");
        }
        state.status = "RUNNING";
        state.pendingCall = null;
        state.facts.remove("pendingApproval");
        events.publish(runId, "APPROVED", "人工已确认写操作");
        return advance(state, true);
    }

    private RunResponse advance(AgentState state, boolean allowWrite) {
        int maxSteps = properties.runtime().maxSteps();
        while (state.steps.size() < maxSteps) {
            AgentDecision decision = planner.next(state);
            if (decision.finished()) {
                state.status = "COMPLETED";
                state.answer = decision.answer();
                return finish(state);
            }
            ToolCall call = decision.call();
            ToolDefinition tool = registry.get(call.toolName());
            int stepNo = state.nextStepNo();
            ToolAuthorization authorization = guard.authorize(allowWrite, tool);
            ToolObservation observation = authorization.allowed()
                    ? registry.call(call) : ToolObservation.blocked(authorization.reason());
            state.record(new AgentStepRecord(stepNo, decision.thought(), call.toolName(),
                    call.arguments(), observation.status(), observation.content(), Instant.now()),
                    observation.facts());
            events.publish(state.runId, "TOOL_" + observation.status(),
                    "step=" + stepNo + " tool=" + call.toolName() + " " + observation.content());
            if ("BLOCKED".equals(observation.status())) {
                state.pendingCall = call;
                state.facts.put("pendingApproval", call.toolName());
                state.status = "WAITING_APPROVAL";
                state.answer = "售后判断已完成，创建补偿工单需人工确认。";
                return finish(state);
            }
            if ("FAILED".equals(observation.status())) {
                state.status = "FAILED";
                state.answer = "工具执行失败，请核对轨迹后重试：" + observation.content();
                return finish(state);
            }
        }
        state.status = "STOPPED";
        state.answer = "达到最大执行步数，运行已停止。";
        return finish(state);
    }

    private RunResponse finish(AgentState state) {
        AgentRunTrace trace = state.snapshot();
        traces.save(trace);
        events.publish(state.runId, state.status, state.answer);
        List<String> history = sessions.get(state.sessionId);
        history.add("助手：" + state.answer);
        while (history.size() > properties.runtime().sessionHistoryLimit()) history.removeFirst();
        return new RunResponse(state.runId, state.sessionId, properties.model().provider(),
                state.status, state.answer, List.copyOf(state.steps), Map.copyOf(state.facts));
    }

    public AgentRunTrace trace(String runId) {
        return traces.findByRunId(runId).orElseThrow(() -> new IllegalArgumentException("找不到运行：" + runId));
    }
    public List<AgentEvent> events(String runId) { requiredState(runId); return events.findByRunId(runId); }
    public SseEmitter subscribe(String runId) { requiredState(runId); return events.subscribe(runId); }
    public List<String> history(String sessionId) { return List.copyOf(sessions.getOrDefault(sessionId, List.of())); }
    private AgentState requiredState(String runId) {
        AgentState state = states.get(runId);
        if (state == null) throw new IllegalArgumentException("找不到运行：" + runId);
        return state;
    }
    private static String extractOrderNo(String message) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\bA[0-9]{3,}\\b").matcher(message);
        return matcher.find() ? matcher.group() : "A100";
    }

    public record AgentRunRequest(String sessionId, String message, boolean allowWrite) {}
    public record RunResponse(String runId, String sessionId, String provider, String status,
                              String answer, List<AgentStepRecord> steps, Map<String, String> facts) {}
    public record AgentStepRecord(int stepNo, String thought, String toolName,
                                  Map<String, String> toolArgs, String status,
                                  String observation, Instant createdAt) {}
    public record AgentRunTrace(String runId, String sessionId, String goal, String orderNo,
                                String status, String answer, List<AgentStepRecord> steps,
                                Map<String, String> facts) {}
    public record AgentEvent(String runId, String type, String message, Instant createdAt) {}

    /** 当前运行事实。工具成功后合并新事实，Planner 只根据这些事实选下一步。 */
    public static final class AgentState {
        private final String runId, sessionId, goal, orderNo;
        private final List<AgentStepRecord> steps = new ArrayList<>();
        private final Map<String, String> facts = new LinkedHashMap<>();
        private String status = "RUNNING", answer = "";
        private ToolCall pendingCall;
        private AgentState(String runId, String sessionId, String goal, String orderNo) {
            this.runId = runId; this.sessionId = sessionId; this.goal = goal; this.orderNo = orderNo;
        }
        public String orderNo() { return orderNo; }
        public boolean hasFact(String key) { return facts.containsKey(key); }
        public String fact(String key) { return facts.get(key); }
        public int nextStepNo() { return steps.size() + 1; }
        public Optional<String> pendingApproval() { return Optional.ofNullable(facts.get("pendingApproval")); }
        private void record(AgentStepRecord step, Map<String, String> newFacts) {
            steps.add(step); facts.putAll(newFacts);
        }
        private AgentRunTrace snapshot() {
            return new AgentRunTrace(runId, sessionId, goal, orderNo, status, answer,
                    List.copyOf(steps), Map.copyOf(facts));
        }
    }

    public interface AgentTraceRepository {
        void save(AgentRunTrace trace);
        Optional<AgentRunTrace> findByRunId(String runId);
    }
    public static final class InMemoryAgentTraceRepository implements AgentTraceRepository {
        private final Map<String, AgentRunTrace> traces = new ConcurrentHashMap<>();
        public void save(AgentRunTrace trace) { traces.put(trace.runId(), trace); }
        public Optional<AgentRunTrace> findByRunId(String runId) { return Optional.ofNullable(traces.get(runId)); }
    }
    public interface AgentEventPublisher {
        void publish(String runId, String type, String message);
        List<AgentEvent> findByRunId(String runId);
        SseEmitter subscribe(String runId);
    }
    public static final class InMemoryAgentEventPublisher implements AgentEventPublisher {
        private final Map<String, List<AgentEvent>> events = new ConcurrentHashMap<>();
        private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();
        public void publish(String runId, String type, String message) {
            AgentEvent event = new AgentEvent(runId, type, message, Instant.now());
            events.computeIfAbsent(runId, ignored -> new ArrayList<>()).add(event);
            for (SseEmitter emitter : subscribers.getOrDefault(runId, List.of())) {
                try { emitter.send(SseEmitter.event().name(type).data(event)); }
                catch (Exception e) { emitter.complete(); subscribers.get(runId).remove(emitter); }
            }
        }
        public List<AgentEvent> findByRunId(String runId) {
            return List.copyOf(events.getOrDefault(runId, List.of()));
        }
        public SseEmitter subscribe(String runId) {
            SseEmitter emitter = new SseEmitter(300_000L);
            subscribers.computeIfAbsent(runId, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(emitter);
            emitter.onCompletion(() -> subscribers.get(runId).remove(emitter));
            emitter.onTimeout(emitter::complete);
            try {
                // 新连接先收到已有事件，再接收后续人工确认产生的新事件。
                for (AgentEvent event : findByRunId(runId))
                    emitter.send(SseEmitter.event().name(event.type()).data(event));
            } catch (Exception e) { emitter.completeWithError(e); }
            return emitter;
        }
    }

    public enum ToolRisk { READ_ONLY, WRITE_REQUIRES_CONFIRMATION }
    public record ToolCall(String toolName, Map<String, String> arguments) {}
    public record ToolObservation(String status, String content, Map<String, String> facts) {
        public static ToolObservation success(String content, Map<String, String> facts) {
            return new ToolObservation("SUCCESS", content, facts);
        }
        public static ToolObservation blocked(String reason) { return new ToolObservation("BLOCKED", reason, Map.of()); }
        public static ToolObservation failed(String reason) { return new ToolObservation("FAILED", reason, Map.of()); }
    }
    public record ToolDefinition(String name, String description, ToolRisk risk,
                                 Function<ToolCall, ToolObservation> handler) {}
    public record ToolAuthorization(boolean allowed, String reason) {
        public static ToolAuthorization permitted() { return new ToolAuthorization(true, ""); }
        public static ToolAuthorization blocked(String reason) { return new ToolAuthorization(false, reason); }
    }
    public static final class ToolPermissionGuard {
        public ToolAuthorization authorize(boolean allowWrite, ToolDefinition tool) {
            return tool.risk() == ToolRisk.WRITE_REQUIRES_CONFIRMATION && !allowWrite
                    ? ToolAuthorization.blocked("工具 " + tool.name() + " 需要人工确认后才能执行")
                    : ToolAuthorization.permitted();
        }
    }
    public static final class ToolRegistry {
        private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
        public void register(ToolDefinition tool) { tools.put(tool.name(), tool); }
        public ToolDefinition get(String name) {
            ToolDefinition tool = tools.get(name);
            if (tool == null) throw new IllegalArgumentException("工具未注册：" + name);
            return tool;
        }
        public ToolObservation call(ToolCall call) {
            try { return get(call.toolName()).handler().apply(call); }
            catch (RuntimeException e) { return ToolObservation.failed("工具执行失败：" + e.getMessage()); }
        }
        public static ToolRegistry afterSales(AfterSalesTools business) {
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ToolDefinition("queryOrder", "查询订单状态", ToolRisk.READ_ONLY, business::queryOrder));
            registry.register(new ToolDefinition("queryLogistics", "查询物流轨迹", ToolRisk.READ_ONLY, business::queryLogistics));
            registry.register(new ToolDefinition("evaluateCompensation", "评估补偿规则", ToolRisk.READ_ONLY, business::evaluateCompensation));
            registry.register(new ToolDefinition("draftCustomerReply", "生成回复口径", ToolRisk.READ_ONLY, business::draftCustomerReply));
            registry.register(new ToolDefinition("createCompensationTicket", "创建补偿工单", ToolRisk.WRITE_REQUIRES_CONFIRMATION, business::createCompensationTicket));
            return registry;
        }
    }

    /** 样例数据和业务规则完全在 Java 中，便于无账号演示。真实项目替换查询接口即可。 */
    public static final class AfterSalesTools {
        private final Map<String, String> tickets = new ConcurrentHashMap<>();
        public ToolObservation queryOrder(ToolCall call) {
            String orderNo = call.arguments().get("orderNo");
            if (!"A100".equals(orderNo) && !"A200".equals(orderNo)) throw new IllegalArgumentException("订单不存在：" + orderNo);
            return ToolObservation.success("订单 " + orderNo + " 已发货", Map.of("orderStatus", "SHIPPED"));
        }
        public ToolObservation queryLogistics(ToolCall call) {
            String orderNo = call.arguments().get("orderNo");
            int delay = "A200".equals(orderNo) ? 2 : 30;
            return ToolObservation.success("物流延迟 " + delay + " 小时", Map.of("logisticsDelayHours", String.valueOf(delay)));
        }
        public ToolObservation evaluateCompensation(ToolCall call) {
            int delay = Integer.parseInt(call.arguments().get("delayHours"));
            boolean eligible = delay >= 24;
            return ToolObservation.success(eligible ? "符合补偿规则" : "暂不符合补偿规则",
                    Map.of("compensationEligible", String.valueOf(eligible)));
        }
        public ToolObservation draftCustomerReply(ToolCall call) {
            String reply = "true".equals(call.arguments().get("eligible"))
                    ? "物流延迟已核实，可申请补偿，工单创建前需要人工确认。" : "已核实物流情况，目前暂不符合补偿规则。";
            return ToolObservation.success(reply, Map.of("customerReply", reply));
        }
        public ToolObservation createCompensationTicket(ToolCall call) {
            String orderNo = call.arguments().get("orderNo");
            String ticket = tickets.computeIfAbsent(orderNo, key -> "TICKET-" + key);
            return ToolObservation.success("已创建补偿工单 " + ticket, Map.of("ticketId", ticket));
        }
    }
    public record AgentDecision(boolean finished, String thought, ToolCall call, String answer) {
        public static AgentDecision callTool(String thought, ToolCall call) {
            return new AgentDecision(false, thought, call, null);
        }
        public static AgentDecision finish(String answer) { return new AgentDecision(true, "", null, answer); }
    }
    public interface Planner { AgentDecision next(AgentState state); }
    /** 状态驱动的离线规划器。替换为模型时仍返回同一个结构化 AgentDecision。 */
    public static final class RulePlanner implements Planner {
        public AgentDecision next(AgentState state) {
            String orderNo = state.orderNo();
            if (!state.hasFact("orderStatus")) return AgentDecision.callTool("先查询订单", new ToolCall("queryOrder", Map.of("orderNo", orderNo)));
            if (!state.hasFact("logisticsDelayHours")) return AgentDecision.callTool("再查物流", new ToolCall("queryLogistics", Map.of("orderNo", orderNo)));
            if (!state.hasFact("compensationEligible")) return AgentDecision.callTool("按规则评估补偿", new ToolCall("evaluateCompensation", Map.of("delayHours", state.fact("logisticsDelayHours"))));
            if (!state.hasFact("customerReply")) return AgentDecision.callTool("整理回复口径", new ToolCall("draftCustomerReply", Map.of("eligible", state.fact("compensationEligible"))));
            if ("true".equals(state.fact("compensationEligible")) && !state.hasFact("ticketId"))
                return AgentDecision.callTool("创建补偿工单", new ToolCall("createCompensationTicket", Map.of("orderNo", orderNo)));
            return AgentDecision.finish(state.fact("customerReply") + (state.hasFact("ticketId") ? " 工单号：" + state.fact("ticketId") : ""));
        }
    }
}
