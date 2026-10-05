package com.example.codingagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 在受限容器中真正编译并执行业务测试，不通过查找源码字符串判断成功。
 * 模型生成代码不在宿主 JVM 加载。输入只读、无网络，受时间和资源限制。
 */
public final class RunTestsTool implements AgentTool {
    private final ContainerRunner runner;
    public RunTestsTool() { this(new BoundedProcessRunner()); }
    public RunTestsTool(ContainerRunner runner) { this.runner = runner; }

    @Override
    public String name() {
        return "run_tests";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.MEDIUM;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        Path source = context.boundary().resolve("src/main/java/com/acme/billing/PriceCalculator.java");
        Path tests = context.boundary().resolve("src/test/java/com/acme/billing/PriceCalculatorTest.java");
        Path input = Files.createTempDirectory("coding-agent-tests-");
        String container = "course-test-" + UUID.randomUUID();

        try {
            Files.copy(source, input.resolve("PriceCalculator.java"));
            Files.copy(tests, input.resolve("PriceCalculatorTest.java"));
            input.toFile().setReadable(true, false);
            input.toFile().setExecutable(true, false);
            for (Path p : List.of(input.resolve("PriceCalculator.java"), input.resolve("PriceCalculatorTest.java")))
                p.toFile().setReadable(true, false);
            ContainerRunner.Result result = runner.run(command(input, container), java.time.Duration.ofSeconds(45), 16_384);
            if (result.outputExceeded()) return ToolResult.failure("TESTS_FAILED", "测试输出超过 16384 字节，已终止容器；未通过。");
            if (result.timedOut()) return ToolResult.failure("TESTS_FAILED", "容器测试超过 45 秒，已终止；未通过。");
            if (result.exitCode() == 125 || result.exitCode() == 126 || result.exitCode() == 127)
                return ToolResult.failure("TEST_ENVIRONMENT_UNAVAILABLE", "Docker Engine 或预置 eclipse-temurin:21-jdk 镜像不可用；未执行成功。");
            if (result.exitCode() != 0) return ToolResult.failure("TESTS_FAILED", result.output());
            if (!result.output().contains("ALL_BILLING_TESTS_PASSED"))
                return ToolResult.failure("TESTS_FAILED", "未收到业务测试完成标记");
            return ToolResult.success("TESTS_PASSED", result.output());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("TEST_CANCELLED", "测试线程被中断，未标记成功");
        } catch (IOException ex) {
            return ToolResult.failure("TEST_ENVIRONMENT_UNAVAILABLE", "需要 Docker CLI、已运行的 Docker Engine 和 eclipse-temurin:21-jdk 镜像");
        } finally {
            try {
                runner.run(List.of("docker", "rm", "-f", container), java.time.Duration.ofSeconds(5), 1024);
            } catch (Exception ignored) { /* 清理失败不覆盖主测试结果，不触发宿主执行回退。 */ }
            try (var paths = Files.walk(input)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    /** 命令固定；模型不能增加 shell 参数、网络能力或宿主挂载。镜像须提前拉取。 */
    static List<String> command(Path input, String container) {
        return List.of("docker", "run", "--rm", "--pull=never", "--name", container,
                "--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges",
                "--memory=512m", "--cpus=1", "--pids-limit=64", "--user=65534:65534",
                "--tmpfs", "/tmp:rw,nosuid,size=128m", "--mount", "type=bind,src=" + input + ",dst=/src,readonly",
                "eclipse-temurin:21-jdk", "sh", "-c",
                "mkdir /tmp/classes && javac -proc:none -d /tmp/classes /src/PriceCalculator.java /src/PriceCalculatorTest.java"
                        + " && java -Xmx128m -cp /tmp/classes com.acme.billing.PriceCalculatorTest");
    }
}
