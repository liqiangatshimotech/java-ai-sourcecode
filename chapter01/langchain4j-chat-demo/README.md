# LangChain4j Chat

项目路径：`java-ai-course-examples/chapter01/langchain4j-chat-demo`。真实 AI Services 与有界流式桥接；256片缓冲、60秒空闲超时及上游取消。

单项目 ZIP 统一包含 `java-ai-course-examples` 顶层目录、根 POM、章节 POM 和本项目。解压后进入该顶层目录，按项目 POM 运行，无需下载其他章节。

```bash
cd java-ai-course-examples
mvn -f chapter01/langchain4j-chat-demo/pom.xml test
# 在本机环境中设置 DEEPSEEK_API_KEY，密钥不要写入代码或文档。
AI_PROVIDER=deepseek mvn -f chapter01/langchain4j-chat-demo/pom.xml spring-boot:run
# 本地模型需先启动 Ollama 并拉取 OLLAMA_MODEL 对应模型。
AI_PROVIDER=ollama mvn -f chapter01/langchain4j-chat-demo/pom.xml spring-boot:run
# OpenAI 使用 OPENAI_API_KEY、OPENAI_MODEL 和 OPENAI_BASE_URL。
AI_PROVIDER=openai mvn -f chapter01/langchain4j-chat-demo/pom.xml spring-boot:run
```

端口：8081。JDK：21。Spring Boot 基础版本：3.5.14；Spring AI：1.1.7；LangChain4j 的具体版本以当前项目 POM 为准（1.3 流式项目为 1.20.0，2.4 分类项目为 1.15.0）。

Spring AI 的兼容服务根地址不带 `/v1`（SDK会追加请求路径）；LangChain4j 的兼容地址以 `/v1` 结尾。各项目的配置工厂会按对应 SDK 处理地址。配置支持独立的 DEEPSEEK_MODEL/BASE_URL、OLLAMA_MODEL/BASE_URL、OPENAI_MODEL/BASE_URL，不要把兼容协议理解为模型能力完全相同。

模型输出需继续经过类型、字段和业务校验。模型自报 confidence 不是校准概率，不作为高风险动作批准依据。配置或模型调用失败必须返回错误，不静默切到其他 Provider。日志不保存密钥和完整敏感输入。

离线测试验证配置与业务契约，不表示真实模型已经连通；真实连通性可运行同章 `environment-check-demo --probe`，或调用本项目业务入口。环境自检包可单独下载。

流式事件：正常结束发送 done；模型异常返回错误，不能当作正常完成。客户端断开后请求上游取消，供应商是否立即停算取决于其客户端实现；不承诺断开后计费立即停止。StreamingBridgeTest 覆盖订阅、取消、超时、慢消费者和脱敏错误。
