package com.example.supportticketcopilot.controller;

import com.example.supportticketcopilot.dto.AnalyzeTicketRequest;
import com.example.supportticketcopilot.dto.CustomerChannel;
import com.example.supportticketcopilot.dto.TicketCopilotResponse;
import com.example.supportticketcopilot.service.TicketCopilotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Validated
@RestController
@RequestMapping("/support/tickets")
public class TicketController {

    private final TicketCopilotService copilotService;

    public TicketController(TicketCopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping("/analyze")
    public Mono<TicketCopilotResponse> analyze(@Valid @RequestBody AnalyzeTicketRequest request) {
        return Mono.fromCallable(() -> copilotService.analyze(request))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(path = "/reply/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamReply(
        @RequestParam
        @NotBlank(message = "content must not be blank")
        @Size(max = 4000, message = "content must be no longer than 4000 characters")
        String content,
        @RequestParam(defaultValue = "WEB") CustomerChannel channel
    ) {
        return copilotService.streamCustomerReply(content, channel)
            .map(chunk -> ServerSentEvent.builder(chunk).event("message").build())
            .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build())
            .onErrorResume(ex -> Flux.just(ServerSentEvent.builder(
                    "AI provider call failed. Check server logs for details.")
                .event("error")
                .build()));
    }
}
