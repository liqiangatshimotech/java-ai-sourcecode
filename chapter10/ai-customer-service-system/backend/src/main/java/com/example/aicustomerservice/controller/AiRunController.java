package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.dto.AiRunResponse;
import com.example.aicustomerservice.service.AiRunService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 运营使用的只读审计接口。 */
@RestController
@RequestMapping("/api/support/ai-runs")
public class AiRunController {

    private final AiRunService aiRunService;

    public AiRunController(AiRunService aiRunService) {
        this.aiRunService = aiRunService;
    }

    @GetMapping
    public List<AiRunResponse> search(
        @RequestParam(required = false) String conversationId,
        @RequestParam(required = false) Boolean handoff,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return aiRunService.search(conversationId, handoff, limit).stream()
            .map(AiRunResponse::from)
            .toList();
    }

    @GetMapping("/{requestId}")
    public AiRunResponse get(@PathVariable String requestId) {
        return AiRunResponse.from(aiRunService.get(requestId));
    }
}
