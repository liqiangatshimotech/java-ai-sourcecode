package com.example.structuredoutput.service;

/** 管理有限重试次数；任务负责解析和校验，调用方决定耗尽后的业务动作。 */
public class StructuredOutputRunner {
    public <T> T runWithRetry(StructuredOutputTask<T> task, int maxAttempts) {
        // 总尝试次数包含第一次，不能传入 0 或负数。
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts 必须大于 0");
        RuntimeException lastError = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                return task.run();
            } catch (RuntimeException ex) {
                // 已取消的线程不能继续发请求；模型适配器负责恢复中断标记。
                if (Thread.currentThread().isInterrupted()) throw ex;
                lastError = ex;
            }
        }
        // 重试器不伪造业务成功结果，由分类服务生成明确的复核标记。
        throw new RetryExhaustedException(lastError);
    }

    @FunctionalInterface
    public interface StructuredOutputTask<T> { T run(); }

    public static class RetryExhaustedException extends IllegalStateException {
        public RetryExhaustedException(RuntimeException cause) {
            super("结构化输出多次失败，进入兜底流程", cause);
        }
    }
}
