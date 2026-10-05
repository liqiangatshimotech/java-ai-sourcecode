package com.example.springaimcpclient.web;

import com.example.springaimcpclient.config.AiProvider;
import com.example.springaimcpclient.service.ChatClientRegistry;
import com.example.springaimcpclient.service.McpToolCatalog;
import com.example.springaimcpclient.service.SupportCopilot;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/support")
public class McpClientController {

    private final SupportCopilot copilot;

    private final McpToolCatalog toolCatalog;

    private final ChatClientRegistry registry;

    public McpClientController(SupportCopilot copilot, McpToolCatalog toolCatalog, ChatClientRegistry registry) {
        this.copilot = copilot;
        this.toolCatalog = toolCatalog;
        this.registry = registry;
    }

    @PostMapping("/ask")
    public SupportAnswerResponse ask(@Valid @RequestBody SupportQuestionRequest request) {
        return new SupportAnswerResponse(
            request.provider(),
            request.question(),
            copilot.answer(request.provider(), request.question()),
            toolCatalog.tools()
        );
    }

    @GetMapping("/tools")
    public List<McpToolView> tools() {
        return toolCatalog.tools();
    }

    @GetMapping("/providers")
    public Set<AiProvider> providers() {
        return registry.configuredProviders();
    }
}
