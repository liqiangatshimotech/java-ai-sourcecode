# environment-check-demo

项目路径：`java-ai-course-examples/chapter01/environment-check-demo`。

我在开始写聊天接口前，先确认四件事：Java 版本够不够、Maven 能不能启动、模型配置是否合理、密钥是否已经设置。这个项目把它们做成一个纯 Java 命令行自检器，默认不会请求模型。配置能读出来和模型确实能连通是两件不同的事，输出会明确区分。

## 从源码包运行

需要 JDK 21+、Maven。以下命令在解压后的 `java-ai-course-examples` 目录执行。首次构建会从 Maven 仓库下载依赖，这与调用模型是不同的网络行为。

```bash
mvn -f chapter01/environment-check-demo/pom.xml test
mvn -f chapter01/environment-check-demo/pom.xml compile exec:java
```

默认只运行本地检查，最后会输出：

```text
MODEL_PROBE [NOT_TESTED] 默认不访问模型；--probe 才发一次小额请求
```

`PASS` 表示该检查项通过；`FAIL` 表示缺少条件或验证失败；`NOT_TESTED` 表示没有执行模型连通验证。没有配置云模型密钥时，`CREDENTIAL` 是 `FAIL`，不能因为其他项通过就认为环境完整。

Java 类按 Java 21 编译。如果当前 JDK 低于 21，可能在 Maven 编译或类加载阶段就失败，无法进入自检程序；此时先安装并切换到 JDK 21+。程序内部也会检查当前 Java feature version。

## DeepSeek、Ollama 与 ChatGPT 配置

默认供应商是 DeepSeek。密钥只放环境变量，不写进代码、POM 或 README。

| 供应商 | AI_PROVIDER | 配置变量 | 默认模型 | 默认地址 |
| --- | --- | --- | --- | --- |
| DeepSeek | `deepseek`，也可不设置 | `DEEPSEEK_API_KEY`、`DEEPSEEK_MODEL`、`DEEPSEEK_BASE_URL` | `deepseek-v4-flash` | `https://api.deepseek.com` |
| Ollama | `ollama` | `OLLAMA_MODEL`、`OLLAMA_BASE_URL`；默认不要求 Key | `qwen3` | `http://localhost:11434/v1` |
| OpenAI / ChatGPT | `openai` 或 `chatgpt` | `OPENAI_API_KEY`、`OPENAI_MODEL`、`OPENAI_BASE_URL` | `gpt-4o-mini` | `https://api.openai.com/v1` |

`AI_MODEL`、`AI_BASE_URL` 可以覆盖供应商设置；Key 优先读取供应商专用变量，再使用可选的 `AI_API_KEY`。密钥已设置只证明本机有值，不证明值正确、账号有权限或还有额度。Ollama 是否已启动、模型是否已下载，也要通过显式探测才能验证。

地址只接受 HTTP(S) 主机地址，拒绝内嵌用户名密码、查询参数、片段和无效端口。自检不会回显不合法地址的原值。根地址会补 `/v1/chat/completions`，以 `/v1` 结束的地址只补 `/chat/completions`；不要把完整接口路径当成 BASE_URL。

## 显式验证模型连通

配置好所选模型后，才运行：

```bash
mvn -f chapter01/environment-check-demo/pom.xml compile exec:java -Dexec.args="--probe"
```

这个开关会发送一次小额真实 Chat Completions 请求，最大输出预算为 16 token，10 秒超时，不自动重试。是否计费及金额由服务商决定；它不查询余额，也不能验证后续业务请求质量。如果 JDK、Maven、地址或密钥检查失败，仍然不会发请求。

| 探测结果 | 含义 |
| --- | --- |
| `PASS` | 本次接口返回完整文本结果，不能据此推断业务质量或未来额度 |
| `AUTH_FAILED` | HTTP 401/403，凭据或访问权限有问题 |
| `RATE_LIMITED` | HTTP 429，限流或配额不足 |
| `MODEL_UNSUPPORTED` | 已识别出模型不存在、未开放或不支持接口的响应 |
| `TIMEOUT` | 连接或请求超时，没有自动重试 |
| `TRUNCATED` | 16 token 预算内未完整结束，不能当成通过；部分推理模型需要另行验证 |
| `REFUSAL` | 模型拒绝这次请求 |
| `HTTP_ERROR` | 其他 HTTP 错误，需要检查兼容地址和请求支持范围 |
| `INVALID_RESPONSE` / `NETWORK_OR_INVALID_RESPONSE` | 响应结构不完整、网络失败、非 JSON 或超过 64 KB |

探测只输出固定分类，不打印模型生成文本、远端错误体或 API Key。进程有失败项时退出码为 1；非法命令行参数退出码为 2；只有本地检查通过但未探测模型时，退出码为 0，输出仍明确为 `NOT_TESTED`，不能把退出码 0 当成模型已经可用。

## 代码与验证范围

- `ModelConfig`：配置 Map、供应商别名、地址校验和密钥脱敏。
- `EnvironmentChecker`：本地检查、前置失败阻断与显式网络探测开关。
- `MavenCheck`：固定执行 `mvn --version`，5 秒限制，不收集环境输出。
- `HttpConnectivityProbe`：Java HTTP 客户端、错误分类、结果校验和接收中的 64 KB 响应上限。
- `EnvironmentCheckDemo`：打印逐项结果与退出状态。

单元测试使用配置 Map、替换的 Maven 检查和本机 HTTP fixture，不依赖个人密钥，不调用真实模型。它们验证默认不请求、前置失败阻断、三种供应商配置、凭据地址拒绝、401/429/模型不支持/超时/拒答/截断/非法响应等行为。真实模型连通必须另行显式 `--probe`，本轮没有进行真实模型调用。
