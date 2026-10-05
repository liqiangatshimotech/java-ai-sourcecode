# AgentScope Java 2 Runtime Demo

项目名称：`agentscope-java-2-runtime-demo`

这个项目对应 `13-1｜AgentScope Java 2.0 生产化实战`。它重点演示 AgentScope Java 2.0 里几件会影响工程写法的事情：

- 模型 provider 已经拆成扩展模块，DeepSeek 和 ChatGPT 走 OpenAI 兼容模型，Ollama 走本地模型扩展。
- `RuntimeContext` 负责把 `userId`、`sessionId` 放到每次调用里，状态不再靠 Agent 实例自己保存。
- `ReActAgent` 仍然适合讲清楚 reasoning、tool、structured output 的核心链路。
- `HarnessAgent` 用于工作区、记忆、上下文压缩、Plan Mode、子 Agent 和沙箱这类生产化能力。

## 运行

默认 provider 是 DeepSeek：

```bash
export DEEPSEEK_API_KEY=sk-your-key
mvn -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml exec:java
```

切换到 ChatGPT：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-4o-mini
mvn -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml exec:java
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3
mvn -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml exec:java
```

## 测试

测试不会请求真实大模型：

```bash
mvn -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml test
```
# 持久化会话恢复实战

新增 `com.example.agentscope2.demo.StateRecoveryDemo`，配合 `com.example.agentscope2.state.VersionedAgentStateStore` 与 `StateRecoveryTest`。使用固定 AgentScope Java 2.0.0-RC5 的真实 `AgentStateStore` 接口，接入 `ReActAgent.builder().stateStore(store)`。

RC5 的接口没有乐观版本参数。我在外层采用一次调用一个快照：框架读取和保存都经过该接口，完成调用后才把全部 key 用一条带版本条件的 SQL 原子提交到 H2 文件数据库。结果只有提交成功后才交给调用方，版本冲突会抛 `VersionConflict`，不自动重试模型或业务动作。

在 `java-ai-course-examples` 目录运行：

```bash
mvn -q -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml test
mvn -q -f chapter13-1/agentscope-java-2-runtime-demo/pom.xml compile exec:java -Dexec.mainClass=com.example.agentscope2.demo.StateRecoveryDemo
```

默认无需密钥，使用真实 ReActAgent 与模拟 Model。第一次存入 T-1001，第二次重建 Agent、Model 和 Store 后仍从模型请求中看到已提交历史。默认数据库为当前工作目录的 `target/state-recovery/course`，`STATE_DB` 可指定其他持久路径；不要把运行数据打进代码 ZIP。

`RUN_MODEL_CALL=true` 后使用现有 CourseModelFactory 的真实调用配置，默认 DeepSeek，也支持 Ollama 与 ChatGPT。此次仅执行离线验证，未发起付费真实模型调用。

数据库主键为三个独立字段 tenant、user、session，避免拼接分隔符造成碰撞。身份必须来自认证后的服务端主体；适配器检查框架传入的 user/session，租户固定绑定，默认匿名槽读取返回空，匿名写入被拒绝。`listSessionIds` 仅枚举本适配器授权的会话，不是管理端全局列表。

删除先调用 `delete(user, session)`，再 `commit()`。内容被清空，版本墓碑保留；旧快照不能在删除后复活历史。版本冲突并不会回滚已发生的外部工具副作用，所以本入口不注册业务写工具。真实补偿和审批仍需业务幂等、事务或 outbox。

测试验证：重建实例恢复消息，tenant/user/session 三维隔离，两个并发快照只有一个提交成功，删除可重读验证且旧写入不能复活。这里覆盖已提交会话恢复，未覆盖执行到一半的 pending tool 恢复或跨进程审批。H2 文件模式是本机教学实现，不能直接作为多机器共享数据库；生产迁移需保留 CAS 条件并补审计、备份与权限控制。
