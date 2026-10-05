package com.example.a2a.protocol;

/**
 * message:send 的响应。
 *
 * <p>A2A 响应可以直接返回 Message，也可以返回 Task。示例只返回 Task，因为客服工单处理是任务型交互。
 */
public record SendMessageResponse(
        Task task,
        Message message) {

    public static SendMessageResponse task(Task task) {
        return new SendMessageResponse(task, null);
    }
}
