package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.dto.CustomerSupportRequest;
import com.example.aicustomerservice.dto.CustomerSupportResponse;
import com.example.aicustomerservice.dto.RuntimeResponse;
import com.example.aicustomerservice.service.AiRunService;
import com.example.aicustomerservice.service.CustomerSupportService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class CustomerSupportController {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportController.class);

    private final CustomerSupportService supportService;
    private final AiRunService aiRunService;
    private final ModelSettings modelSettings;
    private final String storageType;
    private final Clock clock;

    public CustomerSupportController(
        CustomerSupportService supportService,
        AiRunService aiRunService,
        ModelSettings modelSettings,
        @Value("${support.storage.type:postgres}") String storageType,
        Clock clock
    ) {
        this.supportService = supportService;
        this.aiRunService = aiRunService;
        this.modelSettings = modelSettings;
        this.storageType = storageType;
        this.clock = clock;
    }

    @GetMapping("/runtime")
    public RuntimeResponse runtime() {
        return RuntimeResponse.from(modelSettings, storageType);
    }

    @PostMapping("/chat")
    public ResponseEntity<CustomerSupportResponse> chat(
        @RequestHeader(value = "X-Request-Id", required = false) String inboundRequestId,
        @Valid @RequestBody CustomerSupportRequest request
    ) {
        Instant receivedAt = clock.instant();
        long startNanos = System.nanoTime();
        String requestId = normalizeRequestId(inboundRequestId);
        // 提前写入响应头，模型失败时客户端仍能拿到 requestId 查询失败审计。
        var context = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (context instanceof org.springframework.web.context.request.ServletRequestAttributes attributes
            && attributes.getResponse() != null) {
            attributes.getResponse().setHeader("X-Request-Id", requestId);
        }
        aiRunService.begin(requestId, request.sessionId(), modelSettings, receivedAt);
        com.example.aicustomerservice.entity.SupportDecision decision;
        long elapsedMs;
        try {
            decision = supportService.handle(request.sessionId(), request.customerId(), request.question(), request.orderNo());
            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            aiRunService.record(requestId, request.sessionId(), modelSettings, decision, elapsedMs, receivedAt);
        } catch (RuntimeException failure) {
            try {
                aiRunService.failed(requestId, request.sessionId(), modelSettings, receivedAt,
                    failure, (System.nanoTime() - startNanos) / 1_000_000);
            } catch (RuntimeException auditFailure) {
                // RUNNING 快照仍保留，可由巡检识别悬挂请求；不能记录带凭据的异常文本。
                log.error("support.chat final audit failed requestId={}", requestId);
                failure.addSuppressed(auditFailure);
            }
            throw failure;
        }

        log.info(
            "support.chat requestId={} customerId={} sessionId={} intent={} handoff={} elapsedMs={}",
            requestId,
            request.customerId(),
            request.sessionId(),
            decision.intent().type(),
            decision.handoffRequired(),
            elapsedMs
        );

        return ResponseEntity.ok()
            .header("X-Request-Id", requestId)
            .body(CustomerSupportResponse.from(decision, requestId, receivedAt, elapsedMs));
    }

    private String normalizeRequestId(String inboundRequestId) {
        // requestId 由服务器产生，不能允许客户端复用同一个值覆盖其他请求的审计。
        return "REQ-" + UUID.randomUUID().toString();
    }
}
