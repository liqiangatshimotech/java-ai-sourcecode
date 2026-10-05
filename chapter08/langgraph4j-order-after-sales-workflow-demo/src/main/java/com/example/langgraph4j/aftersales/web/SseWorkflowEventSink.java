package com.example.langgraph4j.aftersales.web;

import com.example.langgraph4j.aftersales.workflow.WorkflowEvent;
import com.example.langgraph4j.aftersales.workflow.WorkflowExecutionResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 把 SSE 连接状态与工作流执行状态隔离。
 *
 * <p>浏览器关闭连接后，后续 accept 变成无操作；它不向图运行线程
 * 抛异常，因而 checkpoint、幂等退款和终态仍能正常完成。</p>
 */
final class SseWorkflowEventSink implements Consumer<WorkflowEvent> {

    private final SseEmitter emitter;
    private final AtomicBoolean connected = new AtomicBoolean(true);

    SseWorkflowEventSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void accept(WorkflowEvent event) {
        if (!connected.get()) return;
        try {
            emitter.send(SseEmitter.event()
                    .id(Long.toString(event.sequence()))
                    .name("node")
                    .data(event));
        }
        catch (IOException | IllegalStateException disconnected) {
            connected.set(false);
        }
    }

    void sendResult(WorkflowExecutionResult result) {
        if (!connected.get()) return;
        try {
            emitter.send(SseEmitter.event().name("result").data(result));
            emitter.complete();
        }
        catch (IOException | IllegalStateException disconnected) {
            connected.set(false);
        }
    }

    boolean isConnected() {
        return connected.get();
    }
}
