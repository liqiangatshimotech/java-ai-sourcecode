# 6.2 Spring AI MCP Server Demo

本模块对应课程 `6.2｜Spring AI MCP Server`。

示例使用 Spring AI 1.1.7 MCP Server WebMVC starter，默认以 Streamable HTTP 暴露 MCP endpoint：

- Tool：`query_order_status`、`suggest_refund_action`
- Resource：`policy://refund/{tenantId}`
- Prompt：`refund_assistant`
- 工程关注点：参数校验、租户权限、结果裁剪、审计日志

运行：

```bash
mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml spring-boot:run
```

默认 MCP endpoint：

```text
http://localhost:8082/mcp
```

测试：

```bash
mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml test
```

## 远程 OAuth 资源服务器

默认是 local demo，只能绑定 127.0.0.1，使用 u-1001/acme 固定身份。显式选择 remote profile 后，/mcp 使用 STATELESS 传输，每个请求都验证 Bearer；不会让一个共享 MCP session 携带不同用户身份。

在外部 IdP 注册资源与客户端。准备 MCP_ISSUER、MCP_JWK_SET_URI、MCP_RESOURCE 三个环境变量，再执行 `mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=remote`。配置缺失立即启动失败；生产 URL 使用 HTTPS，HTTP 仅允许回环联调。MCP_RESOURCE 是对外发布的完整资源标识，JWT aud 必须包含它。

服务端通过 Spring Security 验证 JWKS 签名、issuer、exp/nbf（零时钟容差）、audience，要求 sub 与 tenant_id 非空。访问 /mcp 必须有 mcp.read scope。本课程将该 scope 映射为只读客服角色，实际权限映射应由业务策略维护。tenant_id 是受信任 IdP 的声明，不能来自工具参数或普通网关头。

McpSecurityConfiguration 在 HTTP 边界从已验证 Principal 提取 CurrentUser，并复制到 McpTransportContext。CommerceMcpHandlers 的工具、资源和 Prompt 均显式接收该上下文，DemoUserContext 在 remote 模式绝不回退固定用户。工具执行线程不读取 Servlet ThreadLocal 或 SecurityContextHolder。Java 编译已开启 -parameters，保证 MCP 参数名 orderId 与请求一致。

`/.well-known/oauth-protected-resource` 与 `/.well-known/oauth-protected-resource/mcp` 返回 RFC9728 资源元数据；401 的 WWW-Authenticate 包含 resource_metadata 地址，scope 不足返回 403。跨租户订单在业务检查处拒绝，MCP tools/call 返回 isError=true，不把业务拒绝当成成功结果。

测试 `mvn -f chapter06/spring-ai-mcp-server-demo/pom.xml test` 使用测试时生成的 RSA 私钥、本地 JWKS 服务和随机端口真实 HTTP /mcp 请求，覆盖有效令牌、不同租户身份、过期/错误aud/issuer/签名、缺scope、401发现与remote缺配置启动失败。测试密钥不发布到运行配置。授权服务器、PKCE登录页面、令牌刷新/撤销、分布式限流不在本项目中实现。

远程连通性探针位于配套 spring-ai-mcp-client-demo 的 RemoteMcpProbe。先不带 token 查看401发现，再从外部IdP取得资源专用访问令牌并通过环境变量传入。不要把大模型API Key或给其他API签发的token转发到MCP。
