package com.example.agentruntime.web;

import com.example.agentruntime.service.AgentRuntimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** HTTP 层只转换请求和响应，工具权限检查始终在 Runtime 内执行。 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentRuntimeService runtime;
    public AgentController(AgentRuntimeService runtime) { this.runtime = runtime; }

    @PostMapping("/runs")
    public AgentRuntimeService.RunResponse run(@Valid @RequestBody RunRequest request) {
        return runtime.run(new AgentRuntimeService.AgentRunRequest(
                request.sessionId(), request.message(), request.allowWrite()));
    }
    @PostMapping("/runs/{runId}/approve")
    public AgentRuntimeService.RunResponse approve(@PathVariable String runId) { return runtime.approve(runId); }
    @GetMapping("/runs/{runId}/trace")
    public AgentRuntimeService.AgentRunTrace trace(@PathVariable String runId) { return runtime.trace(runId); }
    @GetMapping("/runs/{runId}/events")
    public List<AgentRuntimeService.AgentEvent> events(@PathVariable String runId) { return runtime.events(runId); }
    @GetMapping(value = "/runs/{runId}/events/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable String runId) { return runtime.subscribe(runId); }
    @GetMapping("/sessions/{sessionId}")
    public List<String> history(@PathVariable String sessionId) { return runtime.history(sessionId); }

    public record RunRequest(@NotBlank String sessionId, @NotBlank String message, boolean allowWrite) {}
}
