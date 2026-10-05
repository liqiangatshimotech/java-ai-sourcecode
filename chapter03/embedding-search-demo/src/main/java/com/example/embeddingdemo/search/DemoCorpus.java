package com.example.embeddingdemo.search;

import com.example.embeddingdemo.model.KnowledgeEntry;

import java.util.List;

/**
 * 售后知识库样例数据。
 *
 * <p>这里故意把“发票”写在文档里，把“报销凭证、公司名称不对”写在查询里。
 * 关键词检索很难知道它们其实在说同一个业务问题，Embedding 检索要解决的就是这类语义距离问题。
 */
public final class DemoCorpus {

    private DemoCorpus() {
    }

    public static List<KnowledgeEntry> supportKnowledge() {
        return List.of(
            new KnowledgeEntry(
                "KB-REFUND-001",
                "退款到账时效",
                "订单取消后，退款会原路退回。银行卡支付通常需要 1 到 3 个工作日，余额支付通常实时到账。"
            ),
            new KnowledgeEntry(
                "KB-SHIP-001",
                "收货地址修改",
                "订单未出库前可以修改收货地址。订单已经出库后，需要联系物流侧尝试拦截或改派。"
            ),
            new KnowledgeEntry(
                "KB-INVOICE-001",
                "发票抬头错误怎么办",
                "如果发票抬头填错，可以在订单中心申请重开。原票据会先作废，再按新的抬头重新生成。"
            ),
            new KnowledgeEntry(
                "KB-LOGIN-001",
                "账号无法登录",
                "连续输错密码会触发账号保护。可以通过手机号验证码恢复登录，也可以联系管理员重置密码。"
            )
        );
    }
}
