package com.example.langchain4jchat.service;

import dev.langchain4j.model.chat.response.*;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 将模型回调转换为有明确生命周期的响应流。
 * 订阅时才发请求；慢客户端最多积压256片；断开、超时和溢出均请求取消模型流。
 * 取消句柄可能在首片之后才到达，因此必须记住提前发生的取消，随后立即补取消。
 */
public final class StreamingBridge implements StreamingChatResponseHandler {
    private final Sinks.Many<String> sink = Sinks.many().unicast()
            .onBackpressureBuffer(new ArrayBlockingQueue<>(256));
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicReference<StreamingHandle> handle = new AtomicReference<>();

    public static Flux<String> stream(Consumer<StreamingBridge> start) {
        return Flux.defer(() -> {
            StreamingBridge bridge = new StreamingBridge();
            return bridge.sink.asFlux().timeout(Duration.ofSeconds(60))
                    .doOnSubscribe(subscription -> {
                        try { start.accept(bridge); } catch (RuntimeException ex) { bridge.onError(ex); }
                    }).doFinally(signal -> bridge.cancel());
        });
    }

    @Override public void onPartialResponse(PartialResponse response, PartialResponseContext context) {
        handle.set(context.streamingHandle());
        if (stopped.get()) { context.streamingHandle().cancel(); return; }
        onPartialResponse(response.text());
    }

    @Override public void onPartialResponse(String text) {
        if (stopped.get()) return;
        Sinks.EmitResult result = sink.tryEmitNext(text);
        if (result.isFailure()) {
            sink.tryEmitError(new IllegalStateException("STREAM_CONSUMER_TOO_SLOW"));
            cancel();
        }
    }

    @Override public void onCompleteResponse(ChatResponse response) { sink.tryEmitComplete(); }
    @Override public void onError(Throwable error) {
        // 客户端只获得稳定错误码，不回显供应商错误中的请求内容。
        sink.tryEmitError(new IllegalStateException("MODEL_STREAM_FAILED"));
        cancel();
    }

    private void cancel() {
        stopped.set(true);
        StreamingHandle current = handle.get();
        if (current != null) current.cancel();
    }
}
