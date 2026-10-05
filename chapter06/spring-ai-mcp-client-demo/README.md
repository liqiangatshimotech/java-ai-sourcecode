# 6.3 Spring AI MCP Client Demo

本模块对应课程 `6.3｜Spring AI MCP Client`。

示例演示 Spring AI 应用如何连接 6.2 的 MCP Server，自动发现远端工具，并把 MCP 工具挂到 `ChatClient` 上。

## 先启动 MCP Server

在一个终端启动 6.2 服务端：

```bash
cd java-ai-course-examples
mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml spring-boot:run
```

默认 MCP endpoint：

```text
http://localhost:8082/mcp
```

## 启动 MCP Client

另一个终端启动 6.3 客户端：

```bash
cd java-ai-course-examples
export DEEPSEEK_API_KEY=sk-...
mvn -f chapter06/spring-ai-mcp-client-demo/pom.xml spring-boot:run
```

默认使用 DeepSeek：

```yaml
app.ai.default-provider: deepseek
app.ai.deepseek.base-url: https://api.deepseek.com
app.ai.deepseek.model: deepseek-v4-flash
```

也可以切换到 Ollama 或 ChatGPT：

```bash
APP_AI_DEFAULT_PROVIDER=ollama OLLAMA_MODEL=qwen3.5:9b mvn -f chapter06/spring-ai-mcp-client-demo/pom.xml spring-boot:run
APP_AI_DEFAULT_PROVIDER=chatgpt CHATGPT_API_KEY=sk-... mvn -f chapter06/spring-ai-mcp-client-demo/pom.xml spring-boot:run
```

## 调用示例

```bash
curl -X POST http://localhost:8083/support/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"客户询问 ORD-1001 是否可以退款，请先查订单和退款政策再回答。"}'
```

查看当前发现到的 MCP 工具：

```bash
curl http://localhost:8083/support/tools
```

## 测试

```bash
mvn -f chapter06/spring-ai-mcp-client-demo/pom.xml test
```

## 远程授权联调探针

RemoteMcpProbe 是独立 Java HTTP 入口，连接 6.2 的 remote/STATELESS 服务。原 Spring AI 主应用仍对应本地教学连接，不会自动替用户完成 OAuth 登录。

在 java-ai-course-examples 目录执行：`mvn -f chapter06/spring-ai-mcp-client-demo/pom.xml compile exec:java -Dexec.mainClass=com.example.springaimcpclient.demo.RemoteMcpProbe`。MCP_SERVER_URL 指定完整 /mcp 地址；未配置 MCP_ACCESS_TOKEN 时查看401和资源发现头，取得外部IdP签发的资源专用token后设置 MCP_ACCESS_TOKEN，再运行同一命令读取工具列表。探针不打印token、不跟随重定向。

完整客户端应读取 RFC9728 元数据、发现授权服务器，并完成授权码+PKCE、resource参数、scope申请和安全的令牌存储/刷新。令牌要按用户与资源分开缓存；不要把一个用户的Bearer固化到多用户共用的ChatClient。普通大模型API Key不能当MCP令牌。401需要重新授权或刷新，403应明确告知scope不足，不尝试绕过。

本例只提供已有访问令牌的HTTP联调入口；PKCE授权服务器和交互登录不在项目中实现。服务端RemoteMcpSecurityTest已经用本地签名JWT和真实/mcp请求验证协议与鉴权，不依赖模型API。
