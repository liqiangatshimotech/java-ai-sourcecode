# spring-ai-skill-advisor-demo

我把接口评审 Skill 接到 Spring AI 1.1.7 的真实 `CallAdvisor`。`ChatClient` 负责组装 Advisor 链；匹配时保留已有系统约束，再追加本地 Skill，未匹配时继续原请求。

先在本机设置 `DEEPSEEK_API_KEY`，再运行 `APP_AI_MODE=live APP_AI_DEFAULT_PROVIDER=deepseek mvn -q -f chapter07/spring-ai-skill-advisor-demo/pom.xml compile exec:java`。

| provider | 模型变量及默认值 | 地址变量及默认值 | 密钥变量 |
|---|---|---|---|
| deepseek | DEEPSEEK_MODEL=deepseek-v4-flash | DEEPSEEK_BASE_URL=https://api.deepseek.com | DEEPSEEK_API_KEY |
| ollama | OLLAMA_MODEL=qwen2.5:7b | OLLAMA_BASE_URL=http://localhost:11434 | 无需密钥 |
| chatgpt / openai | OPENAI_MODEL=gpt-4.1-mini | OPENAI_BASE_URL=https://api.openai.com | OPENAI_API_KEY |

通过 APP_AI_DEFAULT_PROVIDER 切换。Spring AI 使用根地址，代码追加 `/v1/chat/completions`，不要重复填写 `/v1`。401 检查对应密钥，404 检查地址和模型名；不要打印密钥排错。

测试验证命中与未命中，以及用户消息和原系统指令保留。此项目只实现同步 CallAdvisor，没有流式 Advisor 或跨请求记忆。Skill 不授予读取文件、运行命令或业务写入权限。

环境要求：JDK 21+、Maven 3.9+。解压后进入 java-ai-course-examples，使用项目 POM 单独运行，不要求下载其他章节。

```bash
mvn -q -f chapter07/spring-ai-skill-advisor-demo/pom.xml test
mvn -q -f chapter07/spring-ai-skill-advisor-demo/pom.xml compile exec:java
```

默认 APP_AI_MODE 未设置时为 offline，仅最后一跳模型使用本地替身，框架调用链真实执行。offline 输出是模拟回答，不代表真实模型效果。APP_AI_MODE=live 才访问模型服务，密钥仅从环境变量读取。

本轮验收仅运行离线测试和启动入口，未发起付费真实模型调用。上线前需在所选服务执行 live 验收。
