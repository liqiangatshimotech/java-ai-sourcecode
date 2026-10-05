package com.example.environmentcheck;

import java.time.Duration;
import java.util.Arrays;

/** 命令行入口：不带参数只查本地，必须显式 --probe 才调用模型。 */
public final class EnvironmentCheckDemo {
    private EnvironmentCheckDemo() { }
    public static void main(String[] args) {
        if (Arrays.stream(args).anyMatch(arg -> !"--probe".equals(arg))) {
            System.err.println("只接受 --probe；默认不请求模型。");
            System.exit(2);
        }
        boolean requested = Arrays.asList(args).contains("--probe");
        var checker = new EnvironmentChecker(MavenCheck.system(), new HttpConnectivityProbe(Duration.ofSeconds(10)));
        var results = checker.check(System.getenv(), Runtime.version().feature(), requested);
        for (CheckResult result : results) System.out.println(result.item() + " [" + result.status() + "] " + result.detail());
        boolean failed = results.stream().anyMatch(result -> result.status() == CheckResult.Status.FAIL);
        boolean untested = results.stream().anyMatch(result -> result.status() == CheckResult.Status.NOT_TESTED);
        System.out.println(failed ? "存在失败项，请按具体结果修复。"
            : untested ? "本地检查通过，模型连通性 NOT_TESTED。" : "本次检查与连通性探测通过。业务质量仍需单独验证。");
        if (failed) System.exit(1);
    }
}
