package com.example.agentruntime.tool;

import com.example.agentruntime.business.AfterSalesTools;
import java.util.LinkedHashMap;
import java.util.Map;
/** 启动时注册工具，运行时只能调用白名单中的 handler。 */
public final class ToolRegistry {
    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    public void register(ToolDefinition tool) {
        if (tools.putIfAbsent(tool.name(), tool) != null) throw new IllegalArgumentException("工具重复注册");
    }
    public ToolDefinition get(String name) {
        ToolDefinition tool = tools.get(name);
        if (tool == null) throw new IllegalArgumentException("工具未注册：" + name);
        return tool;
    }
    public ToolObservation call(ToolCall call) {
        try { return get(call.toolName()).handler().apply(call); }
        catch (RuntimeException e) { return ToolObservation.failed("工具执行失败：" + e.getMessage()); }
    }
    public static ToolRegistry afterSales(AfterSalesTools business) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ToolDefinition("queryOrder", "查询订单状态", ToolRisk.READ_ONLY, business::queryOrder));
        registry.register(new ToolDefinition("queryLogistics", "查询物流轨迹", ToolRisk.READ_ONLY, business::queryLogistics));
        registry.register(new ToolDefinition("evaluateCompensation", "评估补偿规则", ToolRisk.READ_ONLY, business::evaluateCompensation));
        registry.register(new ToolDefinition("draftCustomerReply", "生成回复口径", ToolRisk.READ_ONLY, business::draftCustomerReply));
        registry.register(new ToolDefinition("createCompensationTicket", "创建补偿工单", ToolRisk.WRITE_REQUIRES_CONFIRMATION, business::createCompensationTicket));
        return registry;
    }
}
