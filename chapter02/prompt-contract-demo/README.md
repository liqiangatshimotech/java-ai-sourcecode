# Prompt Contract Demo

本模块对应课程 `2.1｜Prompt 不是玄学，是接口设计`。

它只演示 Prompt 契约如何从业务输入渲染出来，不直接调用模型。

```bash
mvn exec:java
```

课堂上重点看：

- `PromptTemplates.promptContract()`：Prompt 契约结构。
- `PromptTemplates.ticketClassifierPrompt(...)`：工单分类 Prompt 模板。
- `PromptContractDemo`：把用户工单渲染成完整 Prompt。
