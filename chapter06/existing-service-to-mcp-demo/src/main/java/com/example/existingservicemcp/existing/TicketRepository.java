package com.example.existingservicemcp.existing;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class TicketRepository {

    private final Map<String, SupportTicket> tickets = Map.of(
        "TCK-1001", new SupportTicket(
            "TCK-1001",
            "acme",
            "张三",
            CustomerTier.ENTERPRISE,
            "email",
            "发票金额不对",
            "我们企业账户 5 月发票金额和合同不一致，财务今天必须完成报销。",
            TicketStatus.OPEN,
            "内部备注：该客户续费金额较高，需要客户成功经理同步跟进。"
        ),
        "TCK-1002", new SupportTicket(
            "TCK-1002",
            "acme",
            "王五",
            CustomerTier.PRO,
            "web",
            "无法收到验证码",
            "登录后台时一直收不到验证码，已经重试多次。",
            TicketStatus.WAITING_CUSTOMER,
            "内部备注：短信服务供应商今天有抖动。"
        ),
        "TCK-2001", new SupportTicket(
            "TCK-2001",
            "beta",
            "李四",
            CustomerTier.ENTERPRISE,
            "email",
            "API 调用失败",
            "生产 API 从早上开始出现 401，影响线上业务。",
            TicketStatus.OPEN,
            "内部备注：Beta 租户数据，ACME 坐席不能读取。"
        )
    );

    public Optional<SupportTicket> findById(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }
}
