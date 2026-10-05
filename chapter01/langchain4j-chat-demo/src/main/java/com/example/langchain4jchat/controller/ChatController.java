package com.example.langchain4jchat.controller;

import com.example.langchain4jchat.config.AiProvider;
import com.example.langchain4jchat.dto.ChatReply;
import com.example.langchain4jchat.dto.ChatRequest;
import com.example.langchain4jchat.service.ChatClientRegistry;
import com.example.langchain4jchat.service.ChatService;
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
@RequestMapping
public class ChatController {

    private final ChatService chatService;
    private final ChatClientRegistry registry;

    public ChatController(ChatService chatService, ChatClientRegistry registry) {
        this.chatService = chatService;
        this.registry = registry;
    }

    @PostMapping("/chat")
    public Mono<ChatReply> chat(@Valid @RequestBody ChatRequest request) {
        AiProvider provider = registry.resolveProvider(request.provider());

        return Mono.fromCallable(() -> chatService.chat(request.message(), request.provider()))
                .map(content -> new ChatReply(provider.id(), "chat-model", content))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam
            @NotBlank(message = "message must not be blank")
            @Size(max = 8000, message = "message must be no longer than 8000 characters")
            String message,
            @RequestParam(required = false) String provider) {

        return toSse(chatService.stream(message, provider));
    }

    @PostMapping("/assistant")
    public Mono<ChatReply> assistant(@Valid @RequestBody ChatRequest request) {
        AiProvider provider = registry.resolveProvider(request.provider());

        return Mono.fromCallable(() -> chatService.assistantChat(request.message(), request.provider()))
                .map(content -> new ChatReply(provider.id(), "ai-services", content))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(path = "/assistant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> assistantStream(
            @RequestParam
            @NotBlank(message = "message must not be blank")
            @Size(max = 8000, message = "message must be no longer than 8000 characters")
            String message,
            @RequestParam(required = false) String provider) {

        return toSse(chatService.assistantStream(message, provider));
    }

    private Flux<ServerSentEvent<String>> toSse(Flux<String> chunks) {
        return chunks
                .map(chunk -> ServerSentEvent.builder(chunk).event("message").build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build());
    }
}
