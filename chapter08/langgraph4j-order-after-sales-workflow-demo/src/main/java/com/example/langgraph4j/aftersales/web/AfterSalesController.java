package com.example.langgraph4j.aftersales.web;

import com.example.langgraph4j.aftersales.service.AfterSalesWorkflowService;
import com.example.langgraph4j.aftersales.workflow.WorkflowExecutionResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** 售后工作流的同步、恢复、历史和 SSE 接口。 */
@RestController
@RequestMapping("/api/after-sales/workflows")
public class AfterSalesController {

    private final AfterSalesWorkflowService workflowService;
    private final Executor streamExecutor;

    public AfterSalesController(
            AfterSalesWorkflowService workflowService,
            @Qualifier("workflowStreamExecutor") Executor streamExecutor) {
        this.workflowService = workflowService;
        this.streamExecutor = streamExecutor;
    }

    @PostMapping
    public WorkflowExecutionResult start(@Valid @RequestBody StartWorkflowRequest request) {
        return workflowService.start(request.threadId(), request.message());
    }

    @PostMapping("/{threadId}/information")
    public WorkflowExecutionResult provideInformation(
            @PathVariable String threadId,
            @Valid @RequestBody InformationRequest request) {
        return workflowService.provideInformation(threadId, request.message());
    }

    @PostMapping("/{threadId}/approval")
    public WorkflowExecutionResult approve(
            @PathVariable String threadId,
            @Valid @RequestBody ApprovalRequest request) {
        return workflowService.approve(threadId, request.decision(), request.approver());
    }

    @GetMapping("/{threadId}")
    public WorkflowExecutionResult state(@PathVariable String threadId) {
        return workflowService.state(threadId);
    }

    @GetMapping("/{threadId}/history")
    public List<Map<String, Object>> history(@PathVariable String threadId) {
        return workflowService.history(threadId);
    }

    @GetMapping(value = "/diagram", produces = MediaType.TEXT_PLAIN_VALUE)
    public String diagram() {
        return workflowService.mermaid();
    }

    /**
     * 每完成一个图节点就发送一条 SSE；最后再发送 result 事件。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody StartWorkflowRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                SseWorkflowEventSink eventSink = new SseWorkflowEventSink(emitter);
                WorkflowExecutionResult result = workflowService.start(
                        request.threadId(), request.message(), eventSink);
                // 断线后 sink 变成 no-op，上面的图仍会跑到 checkpoint/终态。
                eventSink.sendResult(result);
            }
            catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        }, streamExecutor);
        return emitter;
    }

}
