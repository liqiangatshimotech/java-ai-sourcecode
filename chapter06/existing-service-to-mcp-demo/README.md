# 6.2.1 Existing Service To MCP Demo

本模块演示如何把一个已有 Spring 服务改造成 MCP Server 能力。

改造策略：

- `existing` 包代表原系统：`SupportTicketService`、`TicketController`、`TicketRepository`。
- `mcp` 包代表新增适配层：`SupportTicketMcpAdapter`。
- 原业务服务不依赖 MCP 注解；MCP 只在 Adapter 层负责协议暴露。

运行：

```bash
mvn -f chapter06/existing-service-to-mcp-demo/pom.xml spring-boot:run
```

测试：

```bash
mvn -f chapter06/existing-service-to-mcp-demo/pom.xml test
```
