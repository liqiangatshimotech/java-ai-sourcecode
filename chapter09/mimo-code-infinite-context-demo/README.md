# mimo-code-infinite-context-demo

项目路径：`java-ai-course-examples/chapter09/mimo-code-infinite-context-demo`。

我把“无限上下文”拆成有界窗口中的连续重建：在输入预算耗尽前生成 Checkpoint，下一轮组合 Checkpoint、项目/全局记忆、会话笔记、索引和近期原文；重复且可重新获取的大工具结果可以压缩。这里模拟机制，不承诺模型的一次输入真的无限。

## 单项目代码包运行

在解压后的 `java-ai-course-examples` 目录运行，要求 JDK 21+、Maven：

```bash
mvn -f chapter09/mimo-code-infinite-context-demo/pom.xml compile exec:java
```

入口类：`com.example.chapter09.mimocode.context.Chapter096Demo`。首次构建需要下载依赖。运行不依赖外部 MiMo-Code 仓库，也不发送模型请求。

输出明确标记：

```text
DEMO_MODE=SIMULATED; external source scan=NOT_PERFORMED; model calls=NONE
```

内置对话中的 `SOURCE packages/...`、工具读取和 token 数是解释重建机制的模拟夹具，不能拿它们当成实际源码扫描结果、模型计费记录或真实窗口测量。代码不会去打开这些外部路径。

## 文件与预算

项目在**当前运行目录**的 `target/mimo-code-infinite-context-demo` 自动生成记忆、笔记、索引和 checkpoint。按上述目录运行时，完整位置是 `java-ai-course-examples/target/mimo-code-infinite-context-demo`；下载包不需要预先携带这些产物。

演示窗口配置是算法输入，不是对 DeepSeek、Ollama 或 ChatGPT 当前产品规格的声明。接真实模型时应按所选模型实际输入、输出和推理预算重新配置，并用真实 token 计数与任务质量验证。

## 三种模型配置

默认 `APP_AI_DEFAULT_PROVIDER=deepseek`。可切换 `ollama`、`chatgpt` 或 `openai`，当前只展示配置：

| 供应商 | 变量 |
| --- | --- |
| DeepSeek | `DEEPSEEK_MODEL`、`DEEPSEEK_BASE_URL`、`DEEPSEEK_API_KEY` |
| Ollama | `OLLAMA_MODEL`、`OLLAMA_BASE_URL` |
| ChatGPT / OpenAI | `CHATGPT_MODEL`、`CHATGPT_BASE_URL`、`CHATGPT_API_KEY`（兼容 `OPENAI_API_KEY`） |

Key 仅显示配置状态，不回显字符片段。实际模型连通、摘要质量和长任务效果未由这份离线示例验证。
