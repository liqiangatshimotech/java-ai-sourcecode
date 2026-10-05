# 6.3.1 Existing Service To MCP Client Demo

本模块对应课程 `6.3.1｜把现有 Spring 服务改造成 MCP Client`。

示例演示一个已有售后服务如何在不重写 Controller 和业务编排的前提下，新增 MCP Client 适配层，把原来直接依赖内部系统的查询能力切换为远端 MCP Tool。

## 改造边界

- `existing` 包保留原有服务入口和业务流程。
- `RemoteSupportToolGateway` 是原服务面向外部能力的接口。
- `McpSupportToolGateway` 是新增适配层，通过 Spring AI 的 `ToolCallbackProvider` 调用远端 MCP Tool。

## 运行

先启动 6.2 的 MCP Server：

```bash
cd java-ai-course-examples
mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml spring-boot:run
```

再启动本模块：

```bash
cd java-ai-course-examples
export DEEPSEEK_API_KEY=sk-...
mvn -f chapter06/existing-service-to-mcp-client-demo/pom.xml spring-boot:run
```

调用原有服务入口：

```bash
curl -X POST http://localhost:8084/existing-support/refund-advice \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1001","customerQuestion":"客户询问这笔订单能不能退款"}'
```

## 测试

```bash
mvn -f chapter06/existing-service-to-mcp-client-demo/pom.xml test
```
