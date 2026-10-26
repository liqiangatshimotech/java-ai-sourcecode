package com.example.agentruntime.web;

import com.example.agentruntime.domain.*;
import com.example.agentruntime.service.AgentRuntimeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
/** HTTP 入口只接收请求与返回结果，审批和工具检查仍在 Runtime。 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentRuntimeService runtime;
    public AgentController(AgentRuntimeService runtime) { this.runtime = runtime; }
    @PostMapping("/runs")
    public AgentRunResponse run(@Valid @RequestBody AgentRunRequest request) { return runtime.run(request); }
    @PostMapping("/runs/{runId}/approve")
    public AgentRunResponse approve(@PathVariable String runId) { return runtime.approve(runId); }
    @GetMapping("/runs/{runId}/trace")
    public AgentRunTrace trace(@PathVariable String runId) { return runtime.trace(runId); }
    @GetMapping("/runs/{runId}/events")
    public List<AgentEvent> events(@PathVariable String runId) { return runtime.events(runId); }
    @GetMapping(value = "/runs/{runId}/events/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable String runId) { return runtime.subscribe(runId); }
    @GetMapping("/sessions/{userId}")
    public List<String> history(@PathVariable String userId) { return runtime.history(userId); }
}
