package com.example.environmentcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 配置检查与付费网络探测分开，只有显式 probe=true 且前置条件通过时才发请求。 */
public final class EnvironmentChecker {
    private final MavenCheck maven;
    private final ConnectivityProbe probe;
    public EnvironmentChecker(MavenCheck maven, ConnectivityProbe probe) { this.maven = maven; this.probe = probe; }

    public List<CheckResult> check(Map<String, String> env, int javaFeature, boolean probeRequested) {
        List<CheckResult> results = new ArrayList<>();
        results.add(javaFeature >= 21 ? CheckResult.pass("JDK", "Java " + javaFeature + "，满足 21+")
            : CheckResult.fail("JDK", "需要 JDK 21+，当前版本 " + javaFeature));
        results.add(maven.available() ? CheckResult.pass("MAVEN", "mvn --version 正常退出")
            : CheckResult.fail("MAVEN", "Maven 不可用或 5 秒内未完成，请检查 PATH"));
        ModelConfig config;
        try {
            config = ModelConfig.from(env);
            results.add(CheckResult.pass("CONFIG", config.toString()));
            results.add(config.credentialReady()
                ? CheckResult.pass("CREDENTIAL", config.provider().equals("ollama") ? "Ollama 默认不要求 API Key" : "API Key 已设置，尚未证明有效")
                : CheckResult.fail("CREDENTIAL", "API Key 未设置"));
        } catch (IllegalArgumentException invalid) {
            results.add(CheckResult.fail("CONFIG", invalid.getMessage()));
            results.add(new CheckResult("MODEL_PROBE", CheckResult.Status.NOT_TESTED, "配置不合法，未发请求"));
            return List.copyOf(results);
        }
        if (!probeRequested) {
            results.add(new CheckResult("MODEL_PROBE", CheckResult.Status.NOT_TESTED, "默认不访问模型；--probe 才发一次小额请求"));
        } else if (results.stream().anyMatch(r -> r.status() == CheckResult.Status.FAIL)) {
            results.add(new CheckResult("MODEL_PROBE", CheckResult.Status.NOT_TESTED, "前置检查未通过，未发请求"));
        } else results.add(probe.probe(config));
        return List.copyOf(results);
    }
}
