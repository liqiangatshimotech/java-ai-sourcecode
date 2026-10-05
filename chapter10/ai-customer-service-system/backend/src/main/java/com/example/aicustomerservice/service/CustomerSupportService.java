package com.example.aicustomerservice.service;

import com.example.aicustomerservice.ai.ChatModelClient;
import com.example.aicustomerservice.ai.SupportPrompt;
import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.common.Hashes;
import com.example.aicustomerservice.dao.CustomerDao;
import com.example.aicustomerservice.entity.CustomerProfile;
import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.LogisticsSnapshot;
import com.example.aicustomerservice.entity.ModelCallResult;
import com.example.aicustomerservice.entity.ModelTier;
import com.example.aicustomerservice.entity.OrderSnapshot;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.RefundAssessment;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportDecision;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.ToolTrace;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 客服核心编排。
 *
 * 所有用户请求必须经过这里，而不是让 Controller 直接调用模型。
 * 这样才能把 RAG、Tool、风控、工单和审计串成一个可治理的生产流程。
 */
public class CustomerSupportService {

    private final CustomerDao customerDao;
    private final IntentRecognitionService intentRecognitionService;
    private final RetrievalService retrievalService;
    private final CustomerToolService toolService;
    private final QueuedModelExecutor modelExecutor;
    private final TicketService ticketService;
    private final ConversationMemoryService memoryService;
    private final ProductionGuardrailService guardrailService;
    private final HotQuestionCache hotQuestionCache;

    public CustomerSupportService(
        CustomerDao customerDao,
        IntentRecognitionService intentRecognitionService,
        RetrievalService retrievalService,
        CustomerToolService toolService,
        ChatModelClient chatModelClient,
        TicketService ticketService,
        ConversationMemoryService memoryService
    ) {
        this(
            customerDao,
            intentRecognitionService,
            retrievalService,
            toolService,
            new QueuedModelExecutor(chatModelClient, 16),
            ticketService,
            memoryService,
            new ProductionGuardrailService(),
            new InMemoryHotQuestionCache()
        );
    }

    public CustomerSupportService(
        CustomerDao customerDao,
        IntentRecognitionService intentRecognitionService,
        RetrievalService retrievalService,
        CustomerToolService toolService,
        QueuedModelExecutor modelExecutor,
        TicketService ticketService,
        ConversationMemoryService memoryService,
        ProductionGuardrailService guardrailService,
        HotQuestionCache hotQuestionCache
    ) {
        this.customerDao = customerDao;
        this.intentRecognitionService = intentRecognitionService;
        this.retrievalService = retrievalService;
        this.toolService = toolService;
        this.modelExecutor = modelExecutor;
        this.ticketService = ticketService;
        this.memoryService = memoryService;
        this.guardrailService = guardrailService;
        this.hotQuestionCache = hotQuestionCache;
    }

    public SupportDecision handle(String sessionId, String customerId, String question, String orderNo) {
        CustomerProfile customer = customerDao.findByCustomerId(customerId)
            .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "客户不存在：" + customerId));

        SupportIntent intent = intentRecognitionService.recognize(question);
        List<RetrievedKnowledge> sources = retrievalService.search(question, 3);
        List<ToolTrace> toolTraces = executeTools(customerId, orderNo, intent);
        List<String> memorySummaries = memoryService.recentSummaries(customerId + ":" + sessionId);
        List<ProductionGuardrail> guardrails = new ArrayList<>(
            guardrailService.assess(intent, sources, toolTraces, orderNo, memorySummaries)
        );

        boolean handoffRequired = guardrails.stream().anyMatch(ProductionGuardrail::requiresHandoff);
        SupportPrompt prompt = new SupportPrompt(question, customer, intent, sources, toolTraces, handoffRequired, memorySummaries);
        modelExecutor.client().validateConfiguration();

        // 客户及历史摘要纳入 key，避免把个性化回答复用于其他人；所有证据版本均参与失效。
        String cacheKey = customerId + ":" + Hashes.sha256Short(memorySummaries.toString()) + ":" + cacheKey(intent, question, sources);
        boolean cachedAnswer = false;
        ModelTier modelTier = ModelTier.STANDARD;
        boolean queued = false;
        int queueDepth = 0;

        com.example.aicustomerservice.ai.ModelCompletion completion = null;
        String answer = null;
        if (canUseHotQuestionCache(intent.type(), handoffRequired, toolTraces)) {
            answer = hotQuestionCache.get(cacheKey).orElse(null);
            cachedAnswer = answer != null;
            modelTier = ModelTier.FAST;
        }

        if (answer == null) {
            ModelCallResult modelCall = modelExecutor.generate(prompt);
            answer = modelCall.answer();
            completion = modelCall.completion();
            modelTier = modelCall.tier();
            queued = modelCall.queued();
            queueDepth = modelCall.queueDepth();
            if (queued) {
                guardrails.add(new ProductionGuardrail(
                    EnterpriseProblemType.HIGH_CONCURRENCY_LATENCY,
                    "modelQueueDepth=" + queueDepth,
                    "模型调用进入 QueuedModelExecutor 统一入口，已按队列深度记录并发压力。",
                    false
                ));
            }
            if (canUseHotQuestionCache(intent.type(), handoffRequired, toolTraces)) {
                hotQuestionCache.put(cacheKey, answer);
            }
        }

        SupportTicket ticket = null;
        if (handoffRequired) {
            ticket = ticketService.createOrReuse(
                customerId,
                customer.tier(),
                sessionId,
                question,
                answer,
                intent.type()
            );
            if (ticket.duplicateCount() > 0) {
                guardrails.add(new ProductionGuardrail(
                    EnterpriseProblemType.DUPLICATE_TICKET,
                    "duplicateCount=" + ticket.duplicateCount(),
                    "命中工单幂等键，复用已有工单。",
                    false
                ));
            }
        }

        memoryService.append(customerId + ":" + sessionId, question, answer);
        return new SupportDecision(
            sessionId,
            customerId,
            intent,
            answer,
            handoffRequired,
            ticket,
            sources,
            toolTraces,
            List.copyOf(guardrails),
            cachedAnswer,
            modelTier,
            queued,
            queueDepth,
            modelExecutor.client().settings().safeSummary(),
            completion
        );
    }

    private List<ToolTrace> executeTools(String customerId, String orderNo, SupportIntent intent) {
        List<ToolTrace> traces = new ArrayList<>();
        if (orderNo == null || orderNo.isBlank()) {
            return traces;
        }

        if (intent.type() == IntentType.ORDER_QUERY || intent.type() == IntentType.REFUND) {
            OrderSnapshot order = toolService.findOrder(customerId, orderNo).orElse(null);
            if (order == null) {
                traces.add(new ToolTrace("order-api", "没有查到客户名下订单 " + orderNo));
            } else {
                traces.add(new ToolTrace(
                    "order-api",
                    "订单 " + order.orderNo()
                        + " 当前状态为 " + order.status()
                        + "，金额 " + order.amount()
                        + "，商品 " + order.productSummary()
                ));
            }
        }

        if (intent.type() == IntentType.LOGISTICS_QUERY) {
            if (toolService.findOrder(customerId, orderNo).isEmpty()) {
                traces.add(new ToolTrace("logistics-api", "没有查到客户名下订单，未读取物流信息"));
                return traces;
            }
            LogisticsSnapshot logistics = toolService.findLogistics(orderNo).orElse(null);
            if (logistics == null) {
                traces.add(new ToolTrace("logistics-api", "订单 " + orderNo + " 暂无物流轨迹"));
            } else {
                traces.add(new ToolTrace("logistics-api", logistics.carrier() + " " + logistics.trackingNo() + "：" + logistics.latestEvent()));
            }
        }

        if (intent.type() == IntentType.REFUND) {
            RefundAssessment assessment = toolService.assessRefund(customerId, orderNo);
            traces.add(new ToolTrace("refund-policy-engine", assessment.reason() + assessment.nextStep()));
        }

        return traces;
    }

    private boolean canUseHotQuestionCache(IntentType intent, boolean handoffRequired, List<ToolTrace> toolTraces) {
        return !handoffRequired && toolTraces.isEmpty() && (intent == IntentType.FAQ || intent == IntentType.INVOICE);
    }

    private String cacheKey(SupportIntent intent, String question, List<RetrievedKnowledge> sources) {
        String sourceVersion = sources.stream()
            .map(source -> source.documentId() + "@" + source.version() + ":" + Hashes.sha256Short(source.text()))
            .sorted().collect(java.util.stream.Collectors.joining("|"));
        return intent.type() + ":" + sourceVersion + ":" + Hashes.sha256Short(question);
    }
}
