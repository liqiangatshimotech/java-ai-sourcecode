# Ticket Classification Structured Output Demo

本项目对应课程 `2.2｜Java 后端最需要的是结构化输出`。

它不绑定 Spring AI 或 LangChain4j，专门演示结构化输出的工程闭环：

```text
用户工单 -> Prompt 契约 -> 模型原始输出 -> Jackson 解析 -> Bean Validation 校验 -> 重试/兜底 -> Java DTO
```

## 导入方式

用 IntelliJ IDEA 或 Cursor 打开本目录：

```text
ticket-classification-structured-output-demo
```

JDK 选择 Java 21，然后等待 Maven 依赖加载完成。

## 运行

```bash
mvn test
mvn exec:java
```

默认运行：

```java
com.example.structuredoutput.demo.TicketClassificationDemo
```

课堂上可以重点讲：

- `TicketClassification`：结构化输出 DTO。
- `TicketPromptBuilder`：Prompt 契约。
- `TicketClassificationService`：解析、校验、重试、兜底。
- `FakeModelClient`：模拟第一次输出自然语言失败，第二次输出合法 JSON 成功。
- `AlwaysInvalidModelClient`：模拟多次失败后进入兜底。

## 示例输出

```text
=== Demo 1: 第一次失败，第二次重试成功 ===
...
{
  "category" : "BILLING",
  "priority" : "HIGH",
  "summary" : "用户遇到重复扣费和发票开具失败问题",
  "requiredData" : [ "订单号", "扣费时间", "发票抬头" ],
  "confidence" : 0.86
}

=== Demo 2: 多次失败后进入兜底 ===
...
{
  "category" : "OTHER",
  "priority" : "MEDIUM",
  "summary" : "模型分类失败，转人工处理",
  "requiredData" : [ "人工复核", "原始工单内容" ],
  "confidence" : 0.0
}
```
