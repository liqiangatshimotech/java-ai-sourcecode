package com.example.codingagent.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** HTTP 在接收过程中限制体积，不能等 ofString 全部装进内存后才检查长度。 */
final class BoundedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final int limit;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private Flow.Subscription subscription;
    BoundedBodySubscriber(int limit) { this.limit = limit; }
    @Override public CompletionStage<byte[]> getBody() { return body; }
    @Override public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }
    @Override public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            if (buffer.remaining() > limit - bytes.size()) {
                subscription.cancel();
                body.completeExceptionally(new IOException("MODEL_RESPONSE_TOO_LARGE"));
                return;
            }
            byte[] next = new byte[buffer.remaining()];
            buffer.get(next);
            bytes.writeBytes(next);
        }
        subscription.request(1);
    }
    @Override public void onError(Throwable failure) { body.completeExceptionally(failure); }
    @Override public void onComplete() { body.complete(bytes.toByteArray()); }
}
