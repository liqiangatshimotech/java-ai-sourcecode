package com.example.rerankdemo.model;

import java.util.List;

/**
 * 售后知识库样例。
 *
 * <p>这里故意放了一个干扰项：企业资料里的“公司名称写错”。
 * 查询里出现“报销凭证、公司名称不对、重新开”，弱向量召回容易把它排到发票文档前面。
 */
public final class DemoCorpus {

    private DemoCorpus() {
    }

    public static List<KnowledgeChunk> supportChunks() {
        return List.of(
            new KnowledgeChunk(
                "ACME-INVOICE-001",
                "acme",
                "发票抬头错误后的重开规则",
                "发票抬头填错后，可以在订单中心申请重开。原票据会先作废，再按照新的抬头重新生成。",
                "invoice-policy.md",
                List.of("invoice", "after-sales")
            ),
            new KnowledgeChunk(
                "ACME-COMPANY-001",
                "acme",
                "企业资料中的公司名称写错",
                "企业资料、公司名称、主体名称或营业执照信息写错时，需要重新提交认证资料。资料审核通过后，后台会同步新的企业名称。",
                "company-profile-policy.md",
                List.of("account", "admin")
            ),
            new KnowledgeChunk(
                "ACME-REFUND-001",
                "acme",
                "退款到账时效",
                "订单取消后，退款会原路退回。银行卡支付通常需要 1 到 3 个工作日，余额支付通常实时到账。",
                "refund-policy.md",
                List.of("refund", "after-sales")
            ),
            new KnowledgeChunk(
                "ACME-SHIP-001",
                "acme",
                "订单出库后的地址修改",
                "订单已经出库后，收货地址不能直接修改，需要联系物流侧尝试拦截或改派。",
                "shipping-policy.md",
                List.of("shipping", "after-sales")
            ),
            new KnowledgeChunk(
                "BETA-INVOICE-001",
                "beta",
                "Beta 租户的发票重开规则",
                "Beta 租户的发票重开需要财务人工审核，不适用于 acme 租户。",
                "beta-invoice-policy.md",
                List.of("invoice", "after-sales")
            )
        );
    }
}
