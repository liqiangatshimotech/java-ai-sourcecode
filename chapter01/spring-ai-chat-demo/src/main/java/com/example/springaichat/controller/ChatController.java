package com.example.springaichat.controller;

import com.example.springaichat.dto.ChatReply;
import com.example.springaichat.dto.ChatRequest;
import com.example.springaichat.service.ChatService;
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
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Synchronous question-answer API.
     *
     * <p>ChatClient.call() is blocking, so WebFlux moves it to boundedElastic to keep
     * the event-loop threads responsive.</p>
     */
    @PostMapping
    public Mono<ChatReply> chat(@Valid @RequestBody ChatRequest request) {
        return Mono.fromCallable(() -> chatService.chat(request.message(), request.provider()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * SSE streaming API.
     *
     * <p>Each model chunk is sent as an SSE "message" event. A final "done" event makes
     * browser and command-line clients easy to detect completion.</p>
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam
            @NotBlank(message = "message must not be blank")
            @Size(max = 8000, message = "message must be no longer than 8000 characters")
            String message,
            @RequestParam(required = false) String provider) {

        return chatService.stream(message, provider)
                .map(chunk -> ServerSentEvent.builder(chunk).event("message").build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build())
                .onErrorResume(ex -> Flux.just(ServerSentEvent.builder(
                                "AI provider call failed. Check server logs for details.")
                        .event("error")
                        .build()));
    }
}
