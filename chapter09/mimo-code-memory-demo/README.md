# mimo-code-memory-demo

项目路径：`java-ai-course-examples/chapter09/mimo-code-memory-demo`。

我用 Java 演示原始 History、Markdown 记忆、全文索引、Checkpoint、Dream/Distill、任务进度和写入范围约束。它是机制对照项目，不是 MiMo-Code 本体，也没有读取或运行外部 MiMo-Code 仓库。

## 单项目代码包运行

在解压后的 `java-ai-course-examples` 目录运行，要求 JDK 21+、Maven：

```bash
mvn -f chapter09/mimo-code-memory-demo/pom.xml compile exec:java
```

入口类：`com.example.chapter09.mimocode.Chapter095Demo`。首次构建会下载 Maven 依赖；运行不调用模型、不需要外部源码文件。

输出开头会明确写出：

```text
DEMO_MODE=SIMULATED; external source scan=NOT_PERFORMED; model calls=NONE
```

运行历史里出现的“读取源码”“创建模块”“分析记忆”等事件是内置夹具，用来观察 History、Actor、Checkpoint 如何组织信息，不是这次运行真的完成了源码扫描或文档修改。全文检索用 Java 内存索引说明机制，没有启动 SQLite FTS5。

## 生成的文件

所有产物写在**当前运行目录**的 `target/mimo-code-memory-demo`，包含 History 日志、项目 MEMORY.md、会话 checkpoint.md 和任务 progress.md。按上面的目录运行时，位置是 `java-ai-course-examples/target/mimo-code-memory-demo`。

产物目录在运行时自动创建；单项目包只需要源码、项目 POM 与 Maven 父 POM，不需要事先提供生成的 Markdown 或历史文件。

## 模型配置

默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，保留三类配置位，当前只展示配置，不发送 API 请求：

| 供应商 | 变量 |
| --- | --- |
| DeepSeek | `DEEPSEEK_MODEL`、`DEEPSEEK_BASE_URL`、`DEEPSEEK_API_KEY` |
| Ollama | `OLLAMA_MODEL`、`OLLAMA_BASE_URL`、可选 `OLLAMA_API_KEY` |
| ChatGPT / OpenAI | provider 为 `chatgpt` 或 `openai`；`CHATGPT_MODEL`、`CHATGPT_BASE_URL`、`CHATGPT_API_KEY` |

Key 只从环境读取，输出仅显示是否已配置，不显示字符片段。将来接真实模型时，需要单独验证摘要质量、记忆污染与作用域权限，不能把当前机制演示当成生产能力验收。
