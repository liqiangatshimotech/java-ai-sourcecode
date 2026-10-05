package com.example.codingagent.tool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 从管道并发读取合并输出，内存和日志体积都有硬上限。
 * 超限立即终止 Docker 客户端；RunTestsTool 的 finally 再删除对应容器。
 * 不把输出重定向到无上限宿主文件，也不等待写满管道后才读取。
 */
public final class BoundedProcessRunner implements ContainerRunner {
    @Override
    public Result run(List<String> command, Duration timeout, int outputLimit) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        AtomicBoolean exceeded = new AtomicBoolean();
        FutureTask<byte[]> reader = new FutureTask<>(() -> {
            try (var input = process.getInputStream(); var bytes = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[2048];
                int count;
                while ((count = input.read(chunk)) != -1) {
                    int room = outputLimit - bytes.size();
                    bytes.write(chunk, 0, Math.min(room, count));
                    if (count > room) {
                        exceeded.set(true);
                        process.destroyForcibly();
                        break;
                    }
                }
                return bytes.toByteArray();
            }
        });
        Thread thread = new Thread(reader, "bounded-container-output");
        thread.setDaemon(true);
        thread.start();
        boolean timedOut = false;
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                timedOut = true;
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
            byte[] output;
            try { output = reader.get(2, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
                reader.cancel(true);
                output = "输出读取未完成，不能认定测试通过".getBytes(StandardCharsets.UTF_8);
                timedOut = true;
            }
            return new Result(process.isAlive() ? -1 : process.exitValue(),
                new String(output, StandardCharsets.UTF_8), timedOut, exceeded.get());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            process.getInputStream().close();
            reader.cancel(true);
        }
    }
}
