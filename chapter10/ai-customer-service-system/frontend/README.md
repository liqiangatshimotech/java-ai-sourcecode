# frontend

这是 `ai-customer-service-system` 的零依赖多页面前端。页面由 Spring Boot JAR 直接托管，浏览器和 REST API 使用同一个进程、同一个域名。

## 页面与脚本

| 页面 | 脚本 | 说明 |
| --- | --- | --- |
| `index.html` | `js/overview.js` | 运营驾驶舱 |
| `reception.html` | `js/reception.js` | 会话收件箱、消息时间线、客户 360、AI 建议 |
| `tickets.html` | `js/tickets.js` | 工单列表 |
| `ticket-detail.html` | `js/ticket-detail.js` | 工单协作与时间线 |
| `orders.html` | `js/orders.js` | 订单列表 |
| `order-detail.html` | `js/order-detail.js` | 订单详情和受控状态动作 |
| `knowledge.html` | `js/knowledge.js` | 知识生命周期 |
| `recall.html` | `js/recall.js` | RAG 单条评测 |
| `governance.html` | `js/governance.js` | AI Run 审计与护栏覆盖 |
| `settings.html` | `js/settings.js` | 模型、连接、队列和渠道配置摘要 |

`js/shared.js` 提供统一导航、API 请求、错误处理、运行状态、格式化和通用组件。`styles.css` 是所有页面共用的设计系统。

## 运行方式

推荐从项目根启动 Spring Boot：

```bash
SUPPORT_STORAGE=memory SERVER_PORT=18081 \
mvn -f backend/pom.xml spring-boot:run
```

访问 `http://localhost:18081`。

如果要单独调试静态文件：

```bash
cd frontend
python3 -m http.server 5174
```

再访问 `http://localhost:5174`。前端默认连接 `http://localhost:18081`，也可以在“平台设置 → 开发连接”覆盖地址。

## 交互边界

- 会话必须从后端收件箱进入，不允许手工伪造 session、客户和订单。
- AI 只提供草稿，发送动作必须由坐席确认。
- 内部备注和客户可见回复使用不同 API，不混用可见范围。
- 工单、订单详情使用独立路由，列表页不塞入多个处理表单。
- 模型密钥永远不进入浏览器；设置页只显示后端返回的安全摘要。
- 前端展示的状态动作是业务入口，最终合法性仍由 Java Service 状态机校验。
