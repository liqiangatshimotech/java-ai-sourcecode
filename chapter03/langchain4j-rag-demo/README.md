# 3.3｜LangChain4j RAG 实战

项目名称：`langchain4j-rag-demo`。项目路径：`java-ai-course-examples/chapter03/langchain4j-rag-demo`。

我用本地知识库、Hashing 向量、租户过滤、检索上下文与回答模型串起 RAG。真实回答模型可以替换，但检索仍是教学用 Hashing 向量，不会因为切换聊天模型就自动成为语义 Embedding。

## 默认离线运行

默认 RUN_MODEL_CALL=false。离线 RAG 使用 GroundedChatModel；不会访问外部模型。

以下命令在解压后的 `java-ai-course-examples` 目录执行，要求 JDK 21+、Maven；首次构建需要下载依赖。

```bash
mvn -f chapter03/langchain4j-rag-demo/pom.xml test
mvn -f chapter03/langchain4j-rag-demo/pom.xml compile exec:java
```

## 显式启用真实模型

先把密钥配置在本机环境，再显式开启调用。没有设置供应商时，真实分支使用 **DeepSeek V4 Flash**：

```bash
RUN_MODEL_CALL=true AI_PROVIDER=deepseek mvn -f chapter03/langchain4j-rag-demo/pom.xml compile exec:java
```

这条命令会请求真实模型，可能产生费用。配置缺失或调用失败时会报错，不会自动把规则答案当成真实模型结果。

| 供应商 | AI_PROVIDER | 模型变量与默认值 | 地址与密钥 |
| --- | --- | --- | --- |
| DeepSeek（默认） | `deepseek` | `DEEPSEEK_MODEL=deepseek-v4-flash` | `DEEPSEEK_BASE_URL` 默认 `https://api.deepseek.com/v1`；`DEEPSEEK_API_KEY` |
| Ollama | `ollama` | `OLLAMA_MODEL=qwen2.5:7b` | `OLLAMA_BASE_URL` 默认 `http://localhost:11434/v1`；本地默认不要求 Key |
| OpenAI / ChatGPT | `openai` 或 `chatgpt` | `OPENAI_MODEL=gpt-4.1-mini` | `OPENAI_BASE_URL` 默认 `https://api.openai.com/v1`；`OPENAI_API_KEY` |

三家都走 OpenAI 兼容 Chat Completions 接口；Ollama 需提前启动并准备支持相应能力的模型。`AI_MODEL`、`AI_BASE_URL` 可覆盖专用模型/地址变量，`AI_API_KEY` 可作为密钥兜底；供应商专用 Key 优先。地址禁止内嵌凭据、查询参数和片段，配置打印不显示 Key。密钥不要写进源码、POM 或下载包。

PolicyRagService.createDefault() 永远是测试用离线入口；正常 main 调用 createFromEnvironment()，显式开关才替换真实 ChatModel。

## 代码与验证边界

`CourseChatSettings` 负责配置、默认值、地址校验与脱敏；`CourseChatModelFactory` 负责创建实际模型客户端。业务检索、工具参数校验与审计不因为供应商切换而放宽。

测试保留原有业务/检索用例，新增本机 HTTP fixture，实际走三种供应商及 chatgpt 别名的工厂，验证模型名与 `/v1/chat/completions` 路径。Tool 项目还检查真实请求带有业务工具规格。这些测试不会请求外部模型；本轮未验证真实供应商模型效果或账户权限。
