package com.example.aicustomerservice.service;

import com.example.aicustomerservice.ai.ChatModelClient;
import com.example.aicustomerservice.ai.SupportPrompt;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ModelCallResult;
import com.example.aicustomerservice.entity.ModelTier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import com.example.aicustomerservice.common.BusinessException;

/**
 * 模型请求队列入口。
 *
 * 采用 Semaphore 限制正在执行的 HTTP 调用，超额请求立即返回 MODEL_BUSY。
 * queueDepth 表示当前执行数，tier 只记录业务等级，并不自动切换供应商模型。
 */
public class QueuedModelExecutor {

    private final ChatModelClient chatModelClient;
    private final int maxImmediateRequests;
    private final Semaphore permits;
    private final AtomicInteger inFlight = new AtomicInteger();

    public QueuedModelExecutor(ChatModelClient chatModelClient, int maxImmediateRequests) {
        this.chatModelClient = chatModelClient;
        if (maxImmediateRequests < 1) throw new IllegalArgumentException("并发上限至少为 1");
        this.maxImmediateRequests = maxImmediateRequests;
        this.permits = new Semaphore(maxImmediateRequests, true);
    }

    public ChatModelClient client() {
        return chatModelClient;
    }

    public ModelCallResult generate(SupportPrompt prompt) {
        // 有界入口采用立即拒绝，避免无限等待占满 Web 线程；queued 保留兼容字段且始终为 false。
        if (!permits.tryAcquire()) throw new BusinessException("MODEL_BUSY", "模型并发已达上限，请稍后重试。");
        int depth = inFlight.incrementAndGet();
        try {
            var completion = chatModelClient.generateCompletion(prompt);
            return new ModelCallResult(completion.answer(), tier(prompt), false, depth, completion);
        } finally {
            // HTTP 超时、格式错误和业务异常都必须归还名额，否则系统会越跑越慢。
            inFlight.decrementAndGet();
            permits.release();
        }
    }

    private ModelTier tier(SupportPrompt prompt) {
        if (prompt.customer().tier() == CustomerTier.ENTERPRISE || prompt.intent().type() == IntentType.REFUND) {
            return ModelTier.ESCALATED;
        }
        if (prompt.intent().type() == IntentType.FAQ && !prompt.knowledge().isEmpty()) {
            return ModelTier.FAST;
        }
        return ModelTier.STANDARD;
    }
}
