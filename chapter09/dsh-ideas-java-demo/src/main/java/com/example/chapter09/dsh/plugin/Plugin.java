package com.example.chapter09.dsh.plugin;

/**
 * 插件：对应 Cordis 里的 Service。
 *
 * 一个插件就是一个带 apply(ctx) 方法的对象。它挂载到上下文时，
 * 可以申请 ctx 上的服务、注册新的服务、订阅事件。dsh 里“产品的一切部分
 * 都是插件”——模型适配器、工具注册表、会话日志、甚至默认的 agent loop
 * 本身，都以同样的机制接入，没有谁拥有特权。
 */
@FunctionalInterface
public interface Plugin {

    /**
     * 挂载到上下文。所有注册动作要通过 ctx.effect(...) / ctx.provideService(...)
     * 登记，这样卸载时才能整体撤销。
     */
    void apply(PluginContext ctx);
}
