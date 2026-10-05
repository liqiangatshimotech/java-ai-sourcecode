package com.example.codingagent.tool;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/** 进程边界可注入测试替身；生产实现只由 RunTestsTool 提供固定 Docker 命令。 */
@FunctionalInterface
public interface ContainerRunner {
    Result run(List<String> command, Duration timeout, int outputLimit) throws IOException, InterruptedException;
    record Result(int exitCode, String output, boolean timedOut, boolean outputExceeded) { }
}
