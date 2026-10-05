# langchain4j-mcp-server-demo

第 6.4.1 节示例工程：使用 LangChain4j Community MCP Server，把已有 Java 业务工具发布成 stdio MCP Server。

运行：

```bash
mvn -f chapter06/langchain4j-mcp-server-demo/pom.xml package
java -jar chapter06/langchain4j-mcp-server-demo/target/langchain4j-mcp-server-demo-0.0.1-SNAPSHOT.jar
```

stdio MCP Server 的 stdout 只能输出 JSON-RPC 消息，业务日志应写到 stderr。
