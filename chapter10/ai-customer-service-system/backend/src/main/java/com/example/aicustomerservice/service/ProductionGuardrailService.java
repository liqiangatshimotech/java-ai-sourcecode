package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.ToolTrace;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业生产护栏。
 *
 * 这里把“真实企业会遇到的问题”落成可以在代码里触发和测试的规则。
 * 规则不是为了替代模型，而是控制模型什么时候不能独立给结论。
 */
public class ProductionGuardrailService {

    private static final double MIN_KNOWLEDGE_SCORE = 0.35;
    private static final int MEMORY_SUMMARY_WARNING_SIZE = 5;

    public List<ProductionGuardrail> assess(
        SupportIntent intent,
        List<RetrievedKnowledge> sources,
        List<ToolTrace> toolTraces,
        String orderNo,
        List<String> memorySummaries
    ) {
        List<ProductionGuardrail> guardrails = new ArrayList<>();
        double bestKnowledgeScore = sources.stream()
            .mapToDouble(RetrievedKnowledge::score)
            .max()
            .orElse(0);

        if (bestKnowledgeScore > 0 && bestKnowledgeScore < MIN_KNOWLEDGE_SCORE) {
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.KNOWLEDGE_RECALL_LOW,
                "bestKnowledgeScore=" + bestKnowledgeScore,
                "保留召回证据，但不把低分知识当成确定依据。",
                true
            ));
        }

        if (intent.type() == IntentType.UNKNOWN || (sources.isEmpty() && toolTraces.isEmpty())) {
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.HALLUCINATION_RISK,
                "intent=" + intent.type() + ", sources=" + sources.size() + ", toolTraces=" + toolTraces.size(),
                "不允许模型编造政策，创建工单交给人工确认。",
                true
            ));
        }

        if ((intent.type() == IntentType.ORDER_QUERY || intent.type() == IntentType.LOGISTICS_QUERY)
            && (orderNo == null || orderNo.isBlank())) {
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.HALLUCINATION_RISK,
                "订单或物流问题缺少 orderNo",
                "业务状态必须来自工具查询，缺少订单号时转人工补充信息。",
                true
            ));
        }

        if (toolTraces.stream().anyMatch(trace -> trace.summary().contains("没有查到客户名下订单"))) {
            guardrails.add(new ProductionGuardrail(EnterpriseProblemType.HALLUCINATION_RISK,
                "订单归属校验未通过", "停止读取订单相关数据，由人工核对身份和订单号。", true));
        }

        if (intent.type() == IntentType.REFUND
            && toolTraces.stream().anyMatch(trace -> trace.summary().contains("人工确认"))) {
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.HALLUCINATION_RISK,
                "退款工具返回人工确认",
                "涉及仓库拦截或退款承诺时，创建售后工单。",
                true
            ));
        }

        if (memorySummaries.size() >= MEMORY_SUMMARY_WARNING_SIZE) {
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.CONTEXT_TOO_LONG,
                "memorySummaries=" + memorySummaries.size(),
                "只保留滚动摘要窗口，避免把完整历史塞进模型上下文。",
                false
            ));
        }

        if (!sources.isEmpty()) {
            String versions = sources.stream()
                .map(source -> source.documentId() + "@" + source.version())
                .distinct()
                .toList()
                .toString();
            guardrails.add(new ProductionGuardrail(
                EnterpriseProblemType.KNOWLEDGE_VERSION_DRIFT,
                "sources=" + versions,
                "接口返回知识版本，便于复盘 AI 当时引用的知识。",
                false
            ));
        }

        return guardrails;
    }
}
