package com.example.agentruntime.service;
import com.example.agentruntime.business.*;
import com.example.agentruntime.config.AgentProperties;
import com.example.agentruntime.domain.*;
import com.example.agentruntime.event.*;
import com.example.agentruntime.planner.*;
import com.example.agentruntime.repository.*;
import com.example.agentruntime.tool.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class AgentRuntimeServiceTest {
    private AgentProperties props(int max) { return new AgentProperties(new AgentProperties.Model("deepseek", "", "", ""), new AgentProperties.Runtime(max,20)); }
    private AgentRuntimeService runtime() { return new AgentRuntimeService(props(8)); }
    private AgentRunRequest request(String order, boolean allow) { return new AgentRunRequest("user-a", "查询物流并申请补偿", order, allow); }
    @Test void articleExamplePausesAndResumesWithoutReplayingReads() {
        var runtime=runtime(); var first=runtime.run(request("A20260001",false));
        assertEquals("WAITING_APPROVAL", first.status()); assertEquals(5, first.steps().size());
        assertEquals("BLOCKED", first.steps().getLast().status()); assertFalse(first.pendingApproval().isBlank());
        assertEquals("ELIGIBLE", first.facts().get("compensationDecision")); assertEquals("20", first.facts().get("compensationAmount"));
        var approved=runtime.approve(first.runId()); assertEquals("COMPLETED",approved.status());
        assertEquals("TICKET-A20260001", approved.facts().get("ticketNo")); assertEquals(6, approved.steps().size());
        assertTrue(approved.pendingApproval().isEmpty()); assertTrue(approved.finalAnswer().contains("尚未发放"));
        assertEquals(1, approved.steps().stream().filter(s -> s.toolName().equals("queryOrder")).count());
        assertThrows(IllegalStateException.class, () -> runtime.approve(first.runId()));
        assertEquals(3,runtime.history("user-a").size()); assertTrue(runtime.history("other").isEmpty());
    }
    @Test void noCompensationAndUnknownOrder() {
        assertEquals("WAIT",runtime().run(request("A200",false)).facts().get("compensationDecision"));
        var failed=runtime().run(request("A999",false)); assertEquals("FAILED",failed.status());
        assertEquals("FAILED",failed.steps().getFirst().status());
        assertThrows(IllegalArgumentException.class, () -> runtime().run(request("",false)));
    }
    @Test void exactToolBudgetCanStillFinish() {
        var r=new AgentRuntimeService(props(5)).run(request("A100",true)); assertEquals("COMPLETED",r.status());
        var stopped=new AgentRuntimeService(props(4)).run(request("A100",true)); assertEquals("STOPPED",stopped.status());
        assertFalse(stopped.facts().containsKey("ticketNo"));
    }
    @Test void compensationUsesBothArticleConditionsIncludingTwelveHourBoundary() {
        for (int[] values : List.of(new int[]{48,48,12,0},new int[]{49,48,11,0},new int[]{49,48,12,1})) {
            var tools=new AfterSalesTools(id -> new OrderSnapshot(id,"SHIPPED",values[0],values[1]), id -> new LogisticsSnapshot(id,values[2]));
            var call=new ToolCall("evaluateCompensation",Map.of("orderNo","X"));
            assertEquals(values[3]==1 ? "ELIGIBLE":"WAIT",tools.evaluateCompensation(call).facts().get("compensationDecision"));
            if(values[3]==0) assertThrows(IllegalStateException.class, () -> tools.createCompensationTicket(call));
        }
    }
    @Test void approvalExecutesFrozenCallAndDoesNotAuthorizeNextWrite() {
        ToolRegistry registry=new ToolRegistry(); AtomicInteger one=new AtomicInteger(), two=new AtomicInteger();
        registry.register(new ToolDefinition("writeOne","",ToolRisk.WRITE_REQUIRES_CONFIRMATION,c -> {one.incrementAndGet();return ToolObservation.success("one",Map.of("one","done"));}));
        registry.register(new ToolDefinition("writeTwo","",ToolRisk.WRITE_REQUIRES_CONFIRMATION,c -> {two.incrementAndGet();return ToolObservation.success("two",Map.of("two","done"));}));
        AtomicInteger plans=new AtomicInteger();
        AgentPlanner planner=s -> plans.incrementAndGet()==1 ? AgentDecision.callTool("one",new ToolCall("writeOne",Map.of()))
            : s.hasFact("two") ? AgentDecision.finish("done") : AgentDecision.callTool("two",new ToolCall("writeTwo",Map.of()));
        var runtime=new AgentRuntimeService(props(8),planner,registry,new InMemoryAgentTraceRepository(),new InMemoryAgentEventPublisher());
        var first=runtime.run(request("A100",false)); var next=runtime.approve(first.runId());
        assertEquals(1,one.get()); assertEquals(0,two.get()); assertEquals("WAITING_APPROVAL",next.status());
        assertEquals("writeTwo",next.steps().getLast().toolName());
        assertEquals("COMPLETED",runtime.approve(first.runId()).status()); assertEquals(1,two.get());
    }
    @Test void unregisteredToolAndPlannerFailureReachAuditableTerminalState() {
        for (AgentPlanner planner: List.<AgentPlanner>of(s -> AgentDecision.callTool("bad",new ToolCall("missing",Map.of())),s -> {throw new IllegalStateException("planner unavailable");})) {
            var runtime=new AgentRuntimeService(props(8),planner,new ToolRegistry(),new InMemoryAgentTraceRepository(),new InMemoryAgentEventPublisher());
            var result=runtime.run(request("A100",false)); assertEquals("FAILED",result.status());
            assertEquals("FAILED",runtime.trace(result.runId()).status());
            assertEquals("FAILED",runtime.events(result.runId()).getLast().type());
        }
    }
    @Test void toolArgumentsCannotChangeAfterPause() {
        var mutable=new HashMap<String,String>();mutable.put("orderNo","A100"); var call=new ToolCall("write",mutable);
        mutable.put("orderNo","A200");assertEquals("A100",call.arguments().get("orderNo"));
    }
    @Test void providerDefaultsAndKeysAreSafe() {
        assertEquals("http://localhost:11434/v1", new AgentProperties.Model("ollama","","","").baseUrl());
        assertEquals("https://api.openai.com/v1", new AgentProperties.Model("chatgpt","","","").baseUrl());
        assertFalse(new AgentProperties.Model("deepseek","","","sensitive-test-value").toString().contains("sensitive-test-value"));
        assertThrows(IllegalArgumentException.class, () -> new AgentProperties.Runtime(0,20));
    }
}
