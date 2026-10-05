package com.example.chapter09.dsh.seam;

/**
 * 审批接缝：对应 dsh 的 ctx.approval。
 *
 * 工具流水线在 pre-execute 里判出 ASK 时，会来这里要一次一次性许可。
 * 真实 dsh 里这个请求经 approval/request waterfall 事件分派出去，
 * 由 Web UI 或 ACP 客户端回答；没人回答就按 unavailable 失败。
 * demo 里用控制台模拟“用户点了允许/拒绝”。
 */
@FunctionalInterface
public interface ApprovalSeam {

    /**
     * 发起一次审批询问。
     *
     * @param title    给人看的操作标题
     * @param detail   具体要执行什么（参数、目标路径等）
     * @return true = 允许执行；false = 拒绝
     */
    boolean ask(String title, String detail);
}
