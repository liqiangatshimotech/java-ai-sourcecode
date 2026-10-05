package com.example.environmentcheck;

/** 可注入连接探测，使配置检查测试不必真正请求任何模型。 */
@FunctionalInterface
public interface ConnectivityProbe {
    CheckResult probe(ModelConfig config);
}
