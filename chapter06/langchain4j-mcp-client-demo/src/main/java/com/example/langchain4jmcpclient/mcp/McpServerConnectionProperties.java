package com.example.langchain4jmcpclient.mcp;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record McpServerConnectionProperties(
    String key,
    List<String> stdioCommand,
    Map<String, String> environment,
    String streamableHttpUrl,
    Map<String, String> headers,
    boolean logEvents,
    Duration initializationTimeout,
    Duration toolExecutionTimeout
) {

    public McpServerConnectionProperties {
        key = key == null || key.isBlank() ? "support-mcp-server" : key;
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        initializationTimeout = Objects.requireNonNullElse(initializationTimeout, Duration.ofSeconds(30));
        toolExecutionTimeout = Objects.requireNonNullElse(toolExecutionTimeout, Duration.ofSeconds(60));
    }

    public static McpServerConnectionProperties stdio(String key, List<String> command) {
        return new McpServerConnectionProperties(
            key,
            List.copyOf(command),
            Map.of(),
            null,
            Map.of(),
            false,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60)
        );
    }

    public static McpServerConnectionProperties streamableHttp(String key, String url, Map<String, String> headers) {
        return new McpServerConnectionProperties(
            key,
            List.of(),
            Map.of(),
            url,
            headers,
            false,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60)
        );
    }

    public static McpServerConnectionProperties demoStdioCommand() {
        Path serverJar = Path.of(
            "chapter06",
            "langchain4j-mcp-server-demo",
            "target",
            "langchain4j-mcp-server-demo-0.0.1-SNAPSHOT.jar"
        );
        return stdio("support-mcp-server", List.of(javaExecutable(), "-jar", serverJar.toString()));
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
