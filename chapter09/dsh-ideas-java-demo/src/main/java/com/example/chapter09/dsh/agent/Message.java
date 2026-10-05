package com.example.chapter09.dsh.agent;

/**
 * 对话消息：进入模型请求的最小单位。
 *
 * 注意一个关键区别：Message 是“投影结果”，不是“存储格式”。
 * demo 里真正持久化的是 SessionEvent 追加日志；deriveMessages()
 * 每次从日志重新投影出消息列表。这对应 dsh 的不变量：
 * 模型可见即已记录。
 */
public record Message(String role, String content) {

    @Override
    public String toString() {
        return role + ": " + content;
    }
}
