package com.example.environmentcheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 只在 --probe 授权后发送一次 Chat Completions 请求，不重试、不读取远端余额。
 * 请求最多生成 16 token；模型定价各不相同，因此不承诺免费。
 * 只输出固定错误分类，不输出密钥、原始错误体或模型生成文本。
 */
public final class HttpConnectivityProbe implements ConnectivityProbe {
    private final HttpClient http;
    private final Duration timeout;
    private final ObjectMapper mapper = new ObjectMapper();
    public HttpConnectivityProbe(Duration timeout) {
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override public CheckResult probe(ModelConfig config) {
        if (!config.credentialReady()) return failed("CREDENTIAL_MISSING", "未设置密钥，未发请求");
        try {
            String body = mapper.writeValueAsString(Map.of("model", config.model(), "max_tokens", 16,
                "messages", List.of(Map.of("role", "user", "content", "Reply with OK only."))));
            var builder = HttpRequest.newBuilder(config.endpoint()).timeout(timeout)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (config.apiKeySet()) builder.header("Authorization", "Bearer " + config.apiKeyForRequest());
            HttpResponse<byte[]> response = http.send(builder.build(), info -> new LimitedBody());
            int status = response.statusCode();
            if (status == 401 || status == 403) return failed("AUTH_FAILED", "身份验证或访问权限失败（HTTP " + status + "）");
            if (status == 429) return failed("RATE_LIMITED", "请求被限流或配额不足（HTTP 429），未自动重试");
            if (status < 200 || status >= 300) {
                // 仅利用供应商的标准错误代码分类，不把 message 或其他错误数据输出。
                String code = "";
                String message = "";
                try {
                    JsonNode error = mapper.readTree(response.body()).path("error");
                    code = error.path("code").asText();
                    // 部分本地兼容服务仅返回字符串错误；只用于固定分类，绝不回显原文。
                    message = (error.isTextual() ? error.asText() : error.path("message").asText()).toLowerCase(java.util.Locale.ROOT);
                }
                catch (IOException | NullPointerException ignored) { /* 非 JSON 错误体同样不回显。 */ }
                boolean unsupportedMessage = (status == 400 || status == 404) && message.contains("model")
                    && (message.contains("not found") || message.contains("does not exist")
                        || message.contains("not supported") || message.contains("does not support"));
                if (unsupportedMessage || Set.of("model_not_found", "unsupported_model", "model_not_supported").contains(code))
                    return failed("MODEL_UNSUPPORTED", "模型不存在、未开放给当前账号或不支持此接口");
                return failed("HTTP_ERROR", "接口返回 HTTP " + status + "，检查兼容地址与请求支持范围");
            }
            JsonNode root = mapper.readTree(response.body());
            if (root == null) return failed("INVALID_RESPONSE", "响应为空");
            JsonNode choice = root.path("choices").path(0);
            if ("length".equals(choice.path("finish_reason").asText()))
                return failed("TRUNCATED", "16 token 探测预算内未完整结束，不能认定探测通过");
            JsonNode message = choice.path("message");
            if (message.path("refusal").isTextual() && !message.path("refusal").asText().isBlank())
                return failed("REFUSAL", "模型拒绝本次探测请求");
            if (!"stop".equals(choice.path("finish_reason").asText())
                    || !message.path("content").isTextual() || message.path("content").asText().isBlank())
                return failed("INVALID_RESPONSE", "缺少完整的文本 Chat Completions 结果");
            return CheckResult.pass("MODEL_PROBE", "接口返回完整文本结果；仅证明本次连通，不代表业务质量或后续额度");
        } catch (HttpTimeoutException timeout) {
            return failed("TIMEOUT", "连接或请求超时，未自动重试");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failed("INTERRUPTED", "探测被取消");
        } catch (IOException failure) {
            return failed("NETWORK_OR_INVALID_RESPONSE", "网络失败、JSON 不合法或响应超过 64 KB；原错误不回显");
        } catch (IllegalArgumentException badRequest) {
            return failed("INVALID_REQUEST_CONFIG", "请求配置不合法；原值不回显");
        }
    }

    private CheckResult failed(String category, String detail) {
        return CheckResult.fail("MODEL_PROBE", category + "：" + detail);
    }

    /** 分块接收时累计体积，超过 64 KB 即取消，避免先将超大错误体装入内存。 */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription; subscription.request(1);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > 65_536 - bytes.size()) {
                    subscription.cancel(); result.completeExceptionally(new IOException("response size limit")); return;
                }
                byte[] next = new byte[buffer.remaining()]; buffer.get(next); bytes.writeBytes(next);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
