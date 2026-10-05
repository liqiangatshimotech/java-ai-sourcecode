package com.example.documentextraction;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** 在响应接收过程中限制大小，不能等无限响应全部进内存后才检查长度。 */
final class BoundedResponseSubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();
    private Flow.Subscription subscription;
    private long received;
    public CompletionStage<byte[]> getBody(){return delegate.getBody();}
    public void onSubscribe(Flow.Subscription subscription){this.subscription=subscription;delegate.onSubscribe(subscription);}
    public void onNext(List<ByteBuffer> chunks) {
        for(var chunk:chunks)received+=chunk.remaining();
        if(received>1_000_000) {
            subscription.cancel();delegate.onError(new IllegalArgumentException("响应超过1MB"));
        }else delegate.onNext(chunks);
    }
    public void onError(Throwable error){delegate.onError(error);}
    public void onComplete(){delegate.onComplete();}
}
