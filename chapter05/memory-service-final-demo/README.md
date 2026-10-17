# 5.6｜记忆系统的工程边界

项目名称：`memory-service-final-demo`。使用 JDK 21、Maven 3.9+、Spring Boot 3.5.14、Spring AI 1.1.7。

我把记忆能力放进真实 HTTP 服务，用户和租户来自认证层。短期窗口、长期记忆和 History 分开保存；本项目存储在进程内，重启清空，持久化实现见 5.3/5.5。

在解压后的 java-ai-course-examples 根目录：

- 测试：`mvn -f chapter05/memory-service-final-demo/pom.xml clean test`
- 设置模型密钥、MEMORY_USER_A_PASSWORD、MEMORY_USER_B_PASSWORD 后启动：`mvn -f chapter05/memory-service-final-demo/pom.xml spring-boot:run`
- 另一终端运行 Java 客户端：`java chapter05/memory-service-final-demo/demo/MemoryServiceSmokeClient.java`

默认地址 127.0.0.1:8056，SERVER_PORT 可改端口。客户端默认使用 user-a 及 MEMORY_USER_A_PASSWORD；第二个账号是 user-b。租户由服务环境 MEMORY_TENANT_ID 决定，默认 tenant-1。不要把密码写入代码。

## HTTP 接口

| 方法与路径 | 行为 |
| --- | --- |
| POST /api/memory/{conversation}/chat | JSON 只接收 message，执行真实模型调用并更新记忆 |
| GET /api/memory/{conversation} | 当前登录用户的会话窗口及有效长期记忆 |
| DELETE /api/memory/{conversation} | 只清当前会话窗口 |
| DELETE /api/memory | 清当前用户所有会话窗口及长期记忆 |

所有接口必须通过 HTTP Basic 认证。客户端不能在 JSON 里伪造 userId/tenantId；未知字段返回 400。Basic、内存用户和关闭 CSRF 只用于这个本机非浏览器演示，生产应接既有身份系统、HTTPS 和匹配客户端的 CSRF 策略。

## 代码与边界

- MemoryScope 对各段分别编码，避免冒号拼接碰撞；MemoryAccessPolicy 比较可信身份与目标作用域，非空检查不能当授权。
- MemoryExtractor 只接受少量明确表达；MemoryValidator 限定归一化内容。未匹配不等于恶意，只表示不能自动确认。规则不是通用 Prompt 注入防护，不能声称识别所有攻击。
- 只从原始用户输入提炼，不从 RAG 或助手回复生成偏好。这里只支持固定偏好和一个自述区域样本，没有实现自动摘要审核。
- MemoryRepository 保存最多 10 条短期消息，History 独立追加；长期偏好带来源和 30 天 TTL，同类型更新不会无限重复。
- CustomerSupportMemoryService 在所有读写删除方法校验身份。同步锁覆盖整轮操作，适合单进程演示，不等于多实例串行机制。
- MemoryCleanupService 每 60 秒物理清理到期记录；召回过滤过期内容本身不是物理删除。
- 忘记用户记忆不会自动删除审计 History；History 不会重新注入模型，需要单独制定保留和删除流程。
- 模型调用失败时本轮对话不写入窗口，也不提炼偏好。业务权限、订单状态必须查业务服务。

14 项测试检查实际上下文隔离、伪造身份拒绝、键碰撞、未确认内容、TTL、物理清理、两种删除范围、窗口与 History 分离，以及 HTTP 401/400 和用户隔离。

真实模型默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，另支持 `ollama` 和 `chatgpt`。供应商在启动时选择。

| 供应商 | 环境变量与默认值 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL=https://api.deepseek.com；DEEPSEEK_MODEL=deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL=http://localhost:11434；OLLAMA_MODEL=qwen2.5:7b；需要先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL=https://api.openai.com；CHATGPT_MODEL=gpt-4.1-mini |

密钥只放环境变量或 IDE 运行配置。模型名可以改成账号或本机已安装的模型。测试不调用真实账号，不代表模型、额度或网络已在线验证。

