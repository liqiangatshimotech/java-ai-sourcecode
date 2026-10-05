package com.example.javamcpreporadar.github;

public enum RepositoryRole {
    OFFICIAL_SDK("官方 Java SDK"),
    FRAMEWORK("Java AI 框架"),
    SERVER_COLLECTION("MCP Server 集合"),
    SPECIFIC_SERVER("具体 MCP Server"),
    ANNOTATION_MODEL("注解编程模型");

    private final String label;

    RepositoryRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
