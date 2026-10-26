package com.example.agentruntime.event;

import com.example.agentruntime.domain.AgentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
/** 事件通知与业务状态保存分离；连接断开不影响业务推进。 */
public interface AgentEventPublisher {
    void publish(String runId, String type, String message);
    List<AgentEvent> findByRunId(String runId);
    SseEmitter subscribe(String runId);
}
