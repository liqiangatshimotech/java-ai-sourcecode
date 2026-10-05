package com.example.langchain4jchat.service;

import dev.langchain4j.model.chat.response.*;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

class StreamingBridgeTest {
    @Test void startsOnlyWhenSubscribedAndCompletes() {
        AtomicInteger starts = new AtomicInteger();
        var stream = StreamingBridge.stream(b -> { starts.incrementAndGet(); b.onPartialResponse("hello"); b.onCompleteResponse(null); });
        assertThat(starts).hasValue(0);
        StepVerifier.create(stream).expectNext("hello").verifyComplete();
        assertThat(starts).hasValue(1);
    }

    @Test void disconnectCancelsHandleEvenWhenItArrivesLate() {
        AtomicReference<StreamingBridge> callback = new AtomicReference<>();
        Handle handle = new Handle();
        StepVerifier.create(StreamingBridge.stream(callback::set)).thenCancel().verify();
        callback.get().onPartialResponse(new PartialResponse("late"), new PartialResponseContext(handle));
        assertThat(handle.cancelled).isTrue();
    }

    @Test void idleTimeoutTerminatesStream() {
        StepVerifier.withVirtualTime(() -> StreamingBridge.stream(b -> {}))
                .thenAwait(Duration.ofSeconds(61)).expectError(java.util.concurrent.TimeoutException.class).verify();
    }

    @Test void providerFailureDoesNotLeakProviderBody() {
        StepVerifier.create(StreamingBridge.stream(b -> b.onError(new RuntimeException("secret-body"))))
                .expectErrorMessage("MODEL_STREAM_FAILED").verify();
    }

    @Test void boundedQueueRejectsSlowConsumer() {
        StepVerifier.create(StreamingBridge.stream(b -> {
                    for (int i = 0; i < 300; i++) b.onPartialResponse("x");
                }), 0)
                .thenRequest(300).expectNextCount(256).expectErrorMessage("STREAM_CONSUMER_TOO_SLOW").verify();
    }

    private static class Handle implements StreamingHandle {
        boolean cancelled;
        @Override public void cancel() { cancelled = true; }
        @Override public boolean isCancelled() { return cancelled; }
    }
}
