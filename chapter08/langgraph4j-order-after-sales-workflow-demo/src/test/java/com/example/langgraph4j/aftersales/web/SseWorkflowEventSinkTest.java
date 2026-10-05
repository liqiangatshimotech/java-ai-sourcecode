package com.example.langgraph4j.aftersales.web;

import com.example.langgraph4j.aftersales.domain.WorkflowStatus;
import com.example.langgraph4j.aftersales.workflow.WorkflowEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseWorkflowEventSinkTest {

    @Test
    void disconnectedClientStopsPushWithoutThrowingBackToWorkflow() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("client closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SseWorkflowEventSink sink = new SseWorkflowEventSink(emitter);
        WorkflowEvent event = new WorkflowEvent(
                1, "thread-1", "node-a", WorkflowStatus.RUNNING, "done", Map.of());

        assertDoesNotThrow(() -> sink.accept(event));
        assertFalse(sink.isConnected());
        assertDoesNotThrow(() -> sink.accept(event));
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
