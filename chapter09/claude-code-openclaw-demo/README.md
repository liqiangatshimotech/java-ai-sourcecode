# claude-code-openclaw-demo

项目路径：`java-ai-course-examples/chapter09/claude-code-openclaw-demo`。

我用 Java 分别演示可选的源码目录索引、网关消息分流、文件记忆和 Agent 运行结构。这些演示不需要 TypeScript 运行时，也不调用真实模型。DeepSeek 是默认配置，同时保留 Ollama 和 ChatGPT 配置入口。

## 单项目代码包运行

以下命令在解压后的 `java-ai-course-examples` 目录执行，要求 JDK 21+、Maven；首次构建需要下载 Maven 依赖。

```bash
mvn -f chapter09/claude-code-openclaw-demo/pom.xml test
mvn -f chapter09/claude-code-openclaw-demo/pom.xml compile exec:java
```

入口类是 `com.example.chapter09.Chapter09Demo`。文件记忆产物位于**当前运行目录**的 `target/chapter09-memory-demo/workspace`。按上面的目录运行时，它位于 `java-ai-course-examples/target` 下。

## 外部源码扫描是可选项

单项目 ZIP 不包含第三方源码仓库。默认没有配置 `CLAUDE_CODE_SOURCEMAP_ROOT` 时，会输出：

```text
[1] Claude Code source map [SKIPPED]
未配置有效的 CLAUDE_CODE_SOURCEMAP_ROOT；可选外部源码扫描未执行。
```

后面的 Java 网关、记忆和架构演示继续执行。`SKIPPED` 只表示未扫描，不表示已经核对过任何外部文件。程序不再向课程目录上层自动寻找其他仓库，也不会自动下载源码。

如果已经有可使用的本地源码，可以将 `CLAUDE_CODE_SOURCEMAP_ROOT` 指向对应仓库根，或直接指向 `restored-src/src`。程序只做文件统计和关键路径存在性检查：实际找到源码才报告 `INDEXED`；有效目录内没有源码时报告 `NO_SOURCE_FILES`；读取失败报告 `FAILED`。索引成功不代表目录版本与课程引用一致，也不代表源码语义已经验证。

## 模型配置

`APP_AI_DEFAULT_PROVIDER` 可选 `deepseek`、`ollama`、`chatgpt`，不设置时为 DeepSeek。

| 配置 | 变量 |
| --- | --- |
| DeepSeek | `DEEPSEEK_MODEL`、`DEEPSEEK_BASE_URL` |
| Ollama | `OLLAMA_MODEL`、`OLLAMA_BASE_URL` |
| ChatGPT | `CHATGPT_MODEL`、`CHATGPT_BASE_URL` |

这些配置用于展示结构和模型选择，不产生真实 API 请求。源码扫描测试使用临时夹具，分别验证未配置、目录缺失、空目录及真正有两个源码文件的情况。
