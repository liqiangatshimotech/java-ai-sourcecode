package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.SupportIntent;

/**
 * 意图识别服务。
 *
 * 真实系统可以把这里替换成小模型分类、规则 + 模型混合分类，或者直接走结构化输出。
 * 示例先用关键词规则，保证每次测试结果稳定。
 */
public class IntentRecognitionService {

    public SupportIntent recognize(String question) {
        String text = question == null ? "" : question;
        if (containsAny(text, "退款", "退货", "售后", "赔付")) {
            return new SupportIntent(IntentType.REFUND, 0.86, "命中退款/售后相关关键词");
        }
        if (containsAny(text, "物流", "快递", "运单", "到哪")) {
            return new SupportIntent(IntentType.LOGISTICS_QUERY, 0.82, "命中物流查询关键词");
        }
        if (containsAny(text, "订单", "支付", "购买", "状态")) {
            return new SupportIntent(IntentType.ORDER_QUERY, 0.78, "命中订单查询关键词");
        }
        if (containsAny(text, "发票", "税号", "专票", "抬头")) {
            return new SupportIntent(IntentType.INVOICE, 0.8, "命中发票处理关键词");
        }
        if (containsAny(text, "规则", "政策", "怎么", "如何", "多久")) {
            return new SupportIntent(IntentType.FAQ, 0.62, "命中 FAQ 型问题表达");
        }
        return new SupportIntent(IntentType.UNKNOWN, 0.25, "没有命中稳定意图");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
