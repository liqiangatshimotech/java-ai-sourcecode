package com.example.javamcpreporadar.github;

import java.util.List;

public record KnownJavaMcpRepository(
    String fullName,
    RepositoryRole role,
    String courseNote
) {

    public static List<KnownJavaMcpRepository> defaults() {
        return List.of(
            new KnownJavaMcpRepository(
                "modelcontextprotocol/java-sdk",
                RepositoryRole.OFFICIAL_SDK,
                "官方 Java SDK，适合研究协议实现、服务端和客户端底层能力。"
            ),
            new KnownJavaMcpRepository(
                "spring-projects/spring-ai",
                RepositoryRole.FRAMEWORK,
                "Spring AI 的 MCP starter 和注解能力，适合已有 Spring Boot 服务。"
            ),
            new KnownJavaMcpRepository(
                "quarkiverse/quarkus-mcp-servers",
                RepositoryRole.SERVER_COLLECTION,
                "Quarkus/Java 实现的一组可运行 MCP servers，适合观察现成 server 的组织方式。"
            ),
            new KnownJavaMcpRepository(
                "OpenLinkSoftware/mcp-jdbc-server",
                RepositoryRole.SPECIFIC_SERVER,
                "基于 Java 和 Quarkus 的 JDBC MCP server，适合学习数据库类工具边界。"
            ),
            new KnownJavaMcpRepository(
                "spring-ai-community/mcp-annotations",
                RepositoryRole.ANNOTATION_MODEL,
                "曾作为 Spring AI 社区注解项目孵化，后续能力已进入 Spring AI 2.x+。"
            )
        );
    }
}
