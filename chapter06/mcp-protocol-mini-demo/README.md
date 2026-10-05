# 6.1 MCP Protocol Mini Demo

本模块对应课程 `6.1｜为什么需要 MCP`。

它不依赖 Spring AI 或 LangChain4j MCP SDK，而是用 Jackson 手写一个最小 MCP 协议模拟器，帮助先看清：

- MCP 消息本质是 JSON-RPC 2.0。
- `tools/list` 负责工具发现。
- `tools/call` 负责工具调用。
- `resources/read` 负责读取上下文资源。
- Server 侧必须做参数校验、权限检查、结果裁剪和审计。

运行演示：

```bash
mvn -f chapter06/mcp-protocol-mini-demo/pom.xml exec:java
```

运行测试：

```bash
mvn -f chapter06/mcp-protocol-mini-demo/pom.xml test
```

启动一个按行读写 JSON-RPC 的 stdio Server：

```bash
mvn -f chapter06/mcp-protocol-mini-demo/pom.xml exec:java \
  -Dexec.mainClass=com.example.mcpprotocol.StdioMcpServerApp
```
