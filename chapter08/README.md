# Chapter 08 — Agent 工作流

| 项目 | 路径 | 主题 |
| --- | --- | --- |
| `agent-concepts-demo` | `chapter08/agent-concepts-demo` | 用售后工单说明目标、模型、工具、记忆和护栏 |
| `minimal-agent-loop-demo` | `chapter08/minimal-agent-loop-demo` | 从零实现“思考—行动—观察”循环与停止条件 |
| `spring-boot-agent-runtime-demo` | `chapter08/spring-boot-agent-runtime-demo` | 订单售后 Agent Runtime：状态、轨迹、事件、工具权限、人工确认和 HTTP 接口 |
| `langchain4j-agentic-workflow-demo` | `chapter08/langchain4j-agentic-workflow-demo` | LangChain4j 工具声明与多角色售后协作工作流 |
| `langgraph4j-order-after-sales-workflow-demo` | `chapter08/langgraph4j-order-after-sales-workflow-demo` | LangGraph4j 显式状态图、checkpoint、人工中断与恢复 |

## 与飞书文章对应的运行边界

8.1 单次决策，8.2 完整最小循环，8.3 Spring Boot Runtime，8.4 官方 Agentic 编排，8.5 LangGraph4j 状态图。各项目独立运行，8.3 和 8.5 默认均占用 8080，勿同时使用默认端口。前四个项目 JDK 21+；8.5 独立构建 JDK 17+，整章构建统一使用 JDK 21+。

2026-10-26 核对：8.3 使用文章的 userId/goal/orderNo 请求、compensationDecision/compensationAmount/ticketNo 事实和两个补偿条件。订单 A20260001 与 A100 均可演示等待确认，A200 不补偿。8.4 默认入口是 OfficialAgenticWorkflowDemo，纯 Java 对照不要当作官方 Scope。默认均可离线运行；真实服务调用和 MySQL 恢复应按各自 README 独立验收。
