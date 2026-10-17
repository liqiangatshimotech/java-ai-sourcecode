# 2.2｜Java 后端最需要的是结构化输出

项目名称：`ticket-classification-structured-output-demo`  
项目路径：`java-ai-course-examples/chapter02/ticket-classification-structured-output-demo`

我先用一个故意输出错误的假模型，把工单分类的处理过程跑出来：

用户工单 → 输入校验 → Prompt 契约 → 模型文本 → Jackson 解析 → Bean Validation → 有限重试 / 人工复核标记。

## 环境与运行

使用 JDK 21、Maven 3.9+。项目固定 Spring Boot 3.5.14 的依赖管理，但 Main 不启动 Spring Boot 服务，也没有 HTTP Controller。

保留 ZIP 中的根 POM、chapter02 POM 和项目目录。以下命令在解压后的 `java-ai-course-examples` 目录运行：

- 编译和测试：`mvn -f chapter02/ticket-classification-structured-output-demo/pom.xml clean test`
- 运行文章中的离线示例：`mvn -f chapter02/ticket-classification-structured-output-demo/pom.xml compile exec:java`

也可以进入项目目录后运行 `mvn compile exec:java`。首次运行不能省略 compile。exec-maven-plugin 固定 3.5.1。

入口为 `com.example.structuredoutput.demo.TicketClassificationDemo`。默认无参数使用 `FakeModelClient`：第一次返回自然语言导致解析失败，第二次返回合法 JSON；最终打印 BILLING / HIGH / confidence=0.86 的分类对象，不需要 API Key。

## 对照文章找源码

以下路径都相对 `src/main/java/com/example/structuredoutput/`：

| 文件 | 作用 |
| --- | --- |
| `dto/TicketClassification.java` | 分类结果；五个字段必须提供，confidence 使用 @NotNull Double |
| `dto/ClassifyTicketRequest.java` | 输入不能为空，最多 2000 个 UTF-16 代码单元 |
| `dto/TicketCategory.java`、`dto/TicketPriority.java` | 枚举定义 |
| `prompt/TicketPromptBuilder.java` | 分类规则、JSON 契约、修复提示 |
| `service/TicketClassificationValidator.java` | 显式校验输入和输出，安全提取错误字段 |
| `service/StructuredOutputRunner.java` | 通用有限重试器，实际由分类服务调用 |
| `service/TicketClassificationService.java` | 解析、校验、重试与兜底 |
| `model/ModelClient.java`、`model/FakeModelClient.java` | 模型接口与离线成功演示 |
| `model/AlwaysInvalidModelClient.java` | 用于测试耗尽重试的失败桩 |
| `model/HttpModelClient.java` | 可选的真实供应商适配器 |
| `demo/TicketClassificationDemo.java` | 文章中的 Main，显式装配对象并关闭 ValidatorFactory |

## 可选：调用真实模型

不传参数保持文章的离线演示。显式添加 `-Dexec.args=--live` 后才调用真实模型：

`mvn -f chapter02/ticket-classification-structured-output-demo/pom.xml compile exec:java -Dexec.args=--live`

真实供应商通过 `APP_AI_DEFAULT_PROVIDER` 选择，默认 `deepseek`；另支持 `ollama`、`chatgpt`。

| 供应商 | 配置 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL 默认 https://api.deepseek.com；DEEPSEEK_MODEL 默认 deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL 默认 http://localhost:11434；OLLAMA_MODEL 默认 qwen2.5:7b；本机先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL 默认 https://api.openai.com；CHATGPT_MODEL 默认 gpt-4.1-mini |

密钥只放运行环境。模型名可改成账号或本机可用的模型。DeepSeek/ChatGPT 使用 /v1/chat/completions，Ollama 使用 /api/chat；这里返回的是模型文本，不启用供应商专有的 JSON Schema 强约束。

## 校验与重试的边界

- 普通 Java 方法不会自动执行校验注解。classify 在调用模型前显式检查请求；无效输入直接抛异常，不重试。
- requiredData 不允许缺失或 null，但允许 []；列表元素不能是空白或 null。
- confidence 使用 Double 和 @NotNull，缺失不能被悄悄补成 0.0；合法数值范围是 0～1。
- Jackson 拒绝未知字段、非法枚举和同一输出中的多个 JSON 对象。Bean Validation 检查空值、摘要长度和范围。
- maxAttempts 是包含第一次在内的总尝试次数。解析失败、字段校验失败、模型 RuntimeException 都受同一预算约束；线程中断则直接停止。
- 兜底返回 OTHER / MEDIUM / confidence=0.0，requiredData 包含“人工复核”。这里只产生标记，没有真实人工队列或通知；业务系统不能把它当正常分类直接完成工单。
- 这个小示例没有区分永久性与临时性 HTTP 故障，也没有退避和全局时间预算；接生产服务时还要补这些策略。
- confidence 是模型自报分数，不是经过校准的正确率；结构正确也不能保证业务判断正确。

测试用本机 HTTP 桩验证三种供应商的请求与解析，不需要真实密钥；通过不代表供应商账号、模型和额度已在线验证。
