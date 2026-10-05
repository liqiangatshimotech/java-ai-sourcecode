package com.example.chapter09.dsh.plugin;

/**
 * 释放函数：对应 Cordis 里的 disposer。
 *
 * dsh 的核心承诺是“注册是可逆副作用”——插件往运行时里装的任何东西
 * （一个服务、一段系统提示词、一个工具、一个事件监听器）都必须带回一个
 * 对应的撤销动作。插件被卸载或者整个运行时关闭时，这些动作会被逆序执行，
 * 系统回到“这个插件从没来过”的状态。
 */
@FunctionalInterface
public interface Disposer {

    /** 撤销一次注册。实现里不要再抛出业务异常，只做清理。 */
    void dispose();
}
