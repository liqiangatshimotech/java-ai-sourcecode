CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS cs_order (
    order_no VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    product_summary VARCHAR(255) NOT NULL,
    channel VARCHAR(64) NOT NULL,
    payment_time VARCHAR(32) NOT NULL,
    receiver_masked VARCHAR(255) NOT NULL,
    risk_level VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

COMMENT ON TABLE cs_order IS '客户订单表：模拟订单中心数据，供订单查询、退款评估和客服工具调用。';
COMMENT ON COLUMN cs_order.order_no IS '订单号，业务主键，例如 SO202606210001。';
COMMENT ON COLUMN cs_order.customer_id IS '客户唯一标识，用于校验订单归属。';
COMMENT ON COLUMN cs_order.customer_name IS '客户展示名称。';
COMMENT ON COLUMN cs_order.status IS '订单状态，例如 PAID_PICKING、SHIPPED、DELIVERED、REFUNDING、CLOSED。';
COMMENT ON COLUMN cs_order.amount IS '订单实付金额，保留两位小数。';
COMMENT ON COLUMN cs_order.product_summary IS '商品摘要，供客服展示和退款策略识别商品类型。';
COMMENT ON COLUMN cs_order.channel IS '下单渠道，例如 APP、MINI_PROGRAM、B2B_CONTRACT。';
COMMENT ON COLUMN cs_order.payment_time IS '支付时间文本，保留上游订单系统展示格式。';
COMMENT ON COLUMN cs_order.receiver_masked IS '脱敏收货地址或收货信息。';
COMMENT ON COLUMN cs_order.risk_level IS '订单风险等级或审核标签。';
COMMENT ON COLUMN cs_order.created_at IS '订单创建时间。';
COMMENT ON COLUMN cs_order.updated_at IS '订单最近更新时间。';

CREATE INDEX IF NOT EXISTS idx_cs_order_customer_status ON cs_order (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_cs_order_status_updated ON cs_order (status, updated_at);
CREATE INDEX IF NOT EXISTS idx_cs_order_customer_updated ON cs_order (customer_id, updated_at);

INSERT INTO cs_order (
    order_no, customer_id, customer_name, status, amount, product_summary,
    channel, payment_time, receiver_masked, risk_level, created_at, updated_at
) VALUES
(
    'SO202606210001', 'C10001', '北京朝阳门店', 'PAID_PICKING', 1299.00,
    '企业门店补货套装 x 3', 'B2B_CONTRACT', '2026-06-21 09:42:10',
    '北京市朝阳区 ***', 'HIGH_VALUE',
    '2026-06-21 23:00:00.000000', '2026-06-22 00:30:00.000000'
),
(
    'SO202606210002', 'C10002', '华东 VIP 客户', 'SHIPPED', 299.00,
    '会员精选礼盒 x 1', 'APP', '2026-06-20 18:20:00',
    '杭州市西湖区 ***', 'NORMAL',
    '2026-06-21 01:00:00.000000', '2026-06-22 00:00:00.000000'
),
(
    'SO202606210003', 'C10003', '普通注册用户', 'DELIVERED', 89.00,
    '日用品组合 x 1', 'MINI_PROGRAM', '2026-06-18 12:00:00',
    '上海市浦东新区 ***', 'LOW',
    '2026-06-19 01:00:00.000000', '2026-06-20 01:00:00.000000'
),
(
    'SO202606210004', 'C10001', '北京朝阳门店', 'REFUNDING', 5600.00,
    '企业大客户活动物料 x 20', 'B2B_CONTRACT', '2026-06-19 10:10:00',
    '北京市朝阳区 ***', 'FINANCE_REVIEW',
    '2026-06-20 01:00:00.000000', '2026-06-21 23:30:00.000000'
)
ON CONFLICT (order_no) DO NOTHING;

CREATE TABLE IF NOT EXISTS cs_conversation (
    conversation_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128) NOT NULL,
    customer_tier VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    intent VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL DEFAULT '',
    queue_name VARCHAR(128) NOT NULL,
    assignee VARCHAR(128) NOT NULL DEFAULT '',
    tags TEXT NOT NULL DEFAULT '',
    unread_count INT NOT NULL DEFAULT 0,
    last_message_preview VARCHAR(500) NOT NULL,
    last_message_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    first_response_at TIMESTAMP(6),
    resolved_at TIMESTAMP(6),
    resolution TEXT NOT NULL DEFAULT ''
);

COMMENT ON TABLE cs_conversation IS '客服会话主表：支撑收件箱列表、领取转接、等待和解决状态机。';
COMMENT ON COLUMN cs_conversation.conversation_id IS '会话业务主键，例如 CONV-20260710-001。';
COMMENT ON COLUMN cs_conversation.customer_id IS '客户 ID，用于关联客户画像和订单。';
COMMENT ON COLUMN cs_conversation.customer_name IS '客户展示名称快照，客户中心暂时不可用时列表仍可展示。';
COMMENT ON COLUMN cs_conversation.customer_tier IS '客户等级快照：NORMAL、VIP、ENTERPRISE。';
COMMENT ON COLUMN cs_conversation.channel IS '消息渠道：WEB、WECHAT、APP、EMAIL、HOTLINE。';
COMMENT ON COLUMN cs_conversation.status IS '会话状态：WAITING、IN_PROGRESS、WAITING_CUSTOMER、WAITING_INTERNAL、RESOLVED。';
COMMENT ON COLUMN cs_conversation.priority IS '会话优先级：LOW、NORMAL、HIGH、URGENT。';
COMMENT ON COLUMN cs_conversation.subject IS '客服根据首条消息归纳的会话主题。';
COMMENT ON COLUMN cs_conversation.intent IS '当前主要意图，例如 REFUND、LOGISTICS_QUERY、INVOICE。';
COMMENT ON COLUMN cs_conversation.order_no IS '关联订单号，没有订单时保存空字符串。';
COMMENT ON COLUMN cs_conversation.queue_name IS '当前业务队列，例如售后二线、物流专席。';
COMMENT ON COLUMN cs_conversation.assignee IS '当前处理人，未领取时为空字符串。';
COMMENT ON COLUMN cs_conversation.tags IS '竖线分隔的展示标签，避免第一阶段引入额外标签关系表。';
COMMENT ON COLUMN cs_conversation.unread_count IS '客服尚未阅读的客户消息数量。';
COMMENT ON COLUMN cs_conversation.last_message_preview IS '列表展示的最近一条客户可见消息摘要。';
COMMENT ON COLUMN cs_conversation.last_message_at IS '最近一条客户可见消息时间，用于收件箱排序和等待时长计算。';
COMMENT ON COLUMN cs_conversation.first_response_at IS '首次人工响应时间，用于首响指标。';
COMMENT ON COLUMN cs_conversation.resolved_at IS '会话解决时间，未解决时为空。';
COMMENT ON COLUMN cs_conversation.resolution IS '解决摘要，用于质检和问题分类复盘。';

CREATE INDEX IF NOT EXISTS idx_cs_conversation_status_last_message
    ON cs_conversation (status, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_cs_conversation_assignee_status
    ON cs_conversation (assignee, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_cs_conversation_queue_status
    ON cs_conversation (queue_name, status, priority);
CREATE INDEX IF NOT EXISTS idx_cs_conversation_customer
    ON cs_conversation (customer_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS cs_conversation_message (
    message_id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES cs_conversation (conversation_id) ON DELETE CASCADE,
    sender_type VARCHAR(32) NOT NULL,
    sender VARCHAR(128) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

COMMENT ON TABLE cs_conversation_message IS '客服会话消息表：同时保存客户可见消息和内部协作时间线。';
COMMENT ON COLUMN cs_conversation_message.message_id IS '消息幂等 ID，渠道重复投递时不重复写入。';
COMMENT ON COLUMN cs_conversation_message.conversation_id IS '所属会话 ID。';
COMMENT ON COLUMN cs_conversation_message.sender_type IS '发送方：CUSTOMER、AGENT、AI、SYSTEM。';
COMMENT ON COLUMN cs_conversation_message.sender IS '客户名称、客服名称或系统组件名称。';
COMMENT ON COLUMN cs_conversation_message.visibility IS 'CUSTOMER 为客户可见，INTERNAL 仅客服团队可见。';
COMMENT ON COLUMN cs_conversation_message.content IS '消息或内部事件正文。';

CREATE INDEX IF NOT EXISTS idx_cs_conversation_message_conversation_time
    ON cs_conversation_message (conversation_id, created_at, message_id);

INSERT INTO cs_conversation (
    conversation_id, customer_id, customer_name, customer_tier, channel, status, priority,
    subject, intent, order_no, queue_name, assignee, tags, unread_count,
    last_message_preview, last_message_at, created_at, updated_at,
    first_response_at, resolved_at, resolution
) VALUES
(
    'CONV-20260710-001', 'C10001', '北京朝阳门店', 'ENTERPRISE', 'WECHAT', 'WAITING', 'URGENT',
    '高价值订单取消与仓库拦截', 'REFUND', 'SO202606210004', '售后二线', '',
    '企业客户|高价值订单|退款', 2, '活动物料还没收到，现在可以取消并退款吗？',
    '2026-07-10 09:52:00.000000', '2026-07-10 09:25:00.000000', '2026-07-10 09:52:00.000000',
    NULL, NULL, ''
),
(
    'CONV-20260710-002', 'C10002', '华东 VIP 客户', 'VIP', 'APP', 'IN_PROGRESS', 'HIGH',
    '物流轨迹长时间未更新', 'LOGISTICS_QUERY', 'SO202606210002', '物流专席', '张敏',
    'VIP|物流异常', 0, '我正在为您核对承运商的最新回传。',
    '2026-07-10 09:55:00.000000', '2026-07-10 09:08:00.000000', '2026-07-10 09:55:00.000000',
    '2026-07-10 09:42:00.000000', NULL, ''
),
(
    'CONV-20260710-003', 'C10003', '普通注册用户', 'NORMAL', 'WEB', 'WAITING_CUSTOMER', 'NORMAL',
    '电子发票抬头修改', 'INVOICE', 'SO202606210003', '综合服务', '李杰',
    '发票|等待资料', 0, '请补充正确的企业抬头和税号，我们会继续为您处理。',
    '2026-07-10 09:38:00.000000', '2026-07-10 08:25:00.000000', '2026-07-10 09:39:00.000000',
    '2026-07-10 09:20:00.000000', NULL, ''
),
(
    'CONV-20260710-004', 'C10001', '北京朝阳门店', 'ENTERPRISE', 'EMAIL', 'WAITING_INTERNAL', 'HIGH',
    '企业订单退款进度', 'REFUND', 'SO202606210004', '售后二线', '王芳',
    '企业客户|等待财务', 0, '财务正在复核退款账户，预计今天 16:00 前返回结果。',
    '2026-07-10 09:15:00.000000', '2026-07-10 06:00:00.000000', '2026-07-10 09:15:00.000000',
    '2026-07-10 07:00:00.000000', NULL, ''
),
(
    'CONV-20260710-005', 'C10003', '普通注册用户', 'NORMAL', 'WEB', 'RESOLVED', 'LOW',
    '会员积分到账时间', 'FAQ', '', '综合服务', '李杰',
    'FAQ|已解决', 0, '积分已经补发到账，您可以在会员中心查看。',
    '2026-07-09 14:00:00.000000', '2026-07-09 10:00:00.000000', '2026-07-09 14:00:30.000000',
    '2026-07-09 11:00:00.000000', '2026-07-09 14:00:30.000000',
    '确认积分补发成功，客户无需继续处理。'
)
ON CONFLICT (conversation_id) DO NOTHING;

INSERT INTO cs_conversation_message (
    message_id, conversation_id, sender_type, sender, visibility, content, created_at
) VALUES
('MSG-SEED-001', 'CONV-20260710-001', 'CUSTOMER', '北京朝阳门店', 'CUSTOMER', '活动物料还没收到，现在可以取消并退款吗？', '2026-07-10 09:52:00.000000'),
('MSG-SEED-002', 'CONV-20260710-001', 'AI', 'ai-copilot', 'INTERNAL', '订单金额 5600 元且处于退款复核，需要人工确认仓库拦截结果后再承诺退款。', '2026-07-10 09:53:00.000000'),
('MSG-SEED-003', 'CONV-20260710-002', 'CUSTOMER', '华东 VIP 客户', 'CUSTOMER', '物流两天没有更新，礼盒现在到哪里了？', '2026-07-10 09:28:00.000000'),
('MSG-SEED-004', 'CONV-20260710-002', 'SYSTEM', 'system', 'INTERNAL', '张敏领取了会话。', '2026-07-10 09:41:00.000000'),
('MSG-SEED-005', 'CONV-20260710-002', 'AGENT', '张敏', 'CUSTOMER', '我正在为您核对承运商的最新回传。', '2026-07-10 09:55:00.000000'),
('MSG-SEED-006', 'CONV-20260710-003', 'CUSTOMER', '普通注册用户', 'CUSTOMER', '发票抬头写错了，可以改成公司名称吗？', '2026-07-10 08:50:00.000000'),
('MSG-SEED-007', 'CONV-20260710-003', 'AGENT', '李杰', 'CUSTOMER', '请补充正确的企业抬头和税号，我们会继续为您处理。', '2026-07-10 09:38:00.000000'),
('MSG-SEED-008', 'CONV-20260710-003', 'SYSTEM', '李杰', 'INTERNAL', '会话已设为等待客户：等待开票资料。', '2026-07-10 09:39:00.000000'),
('MSG-SEED-009', 'CONV-20260710-004', 'CUSTOMER', '北京朝阳门店', 'CUSTOMER', '退款申请昨天提交了，什么时候能到账？', '2026-07-10 07:00:00.000000'),
('MSG-SEED-010', 'CONV-20260710-004', 'AGENT', '王芳', 'INTERNAL', '已提交财务复核，关联订单 SO202606210004。', '2026-07-10 08:00:00.000000'),
('MSG-SEED-011', 'CONV-20260710-004', 'AGENT', '王芳', 'CUSTOMER', '财务正在复核退款账户，预计今天 16:00 前返回结果。', '2026-07-10 09:15:00.000000'),
('MSG-SEED-012', 'CONV-20260710-005', 'CUSTOMER', '普通注册用户', 'CUSTOMER', '昨天的积分怎么还没到账？', '2026-07-09 11:00:00.000000'),
('MSG-SEED-013', 'CONV-20260710-005', 'AGENT', '李杰', 'CUSTOMER', '积分已经补发到账，您可以在会员中心查看。', '2026-07-09 14:00:00.000000'),
('MSG-SEED-014', 'CONV-20260710-005', 'SYSTEM', '李杰', 'INTERNAL', '会话已解决：确认积分补发成功，客户无需继续处理。', '2026-07-09 14:00:30.000000')
ON CONFLICT (message_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS cs_ai_run (
    request_id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(128) NOT NULL,
    intent VARCHAR(64) NOT NULL,
    confidence NUMERIC(6, 5) NOT NULL,
    source_summaries JSONB NOT NULL DEFAULT '[]'::jsonb,
    tool_summaries JSONB NOT NULL DEFAULT '[]'::jsonb,
    guardrail_summaries JSONB NOT NULL DEFAULT '[]'::jsonb,
    elapsed_ms BIGINT NOT NULL,
    handoff BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

COMMENT ON TABLE cs_ai_run IS 'AI 客服运行审计表：按 requestId 追溯模型、证据、工具、护栏和处理耗时。';
COMMENT ON COLUMN cs_ai_run.request_id IS '与 /api/support/chat 响应一致的请求追踪 ID。';
COMMENT ON COLUMN cs_ai_run.conversation_id IS '会话 ID；兼容旧接口时也可能是 sessionId。';
COMMENT ON COLUMN cs_ai_run.provider IS '模型供应商：DEEPSEEK、OLLAMA、CHATGPT。';
COMMENT ON COLUMN cs_ai_run.model IS '实际调用的模型名称。';
COMMENT ON COLUMN cs_ai_run.intent IS '本次识别出的客服意图。';
COMMENT ON COLUMN cs_ai_run.confidence IS '意图置信度。';
COMMENT ON COLUMN cs_ai_run.source_summaries IS '知识来源及版本摘要，不保存完整 Prompt。';
COMMENT ON COLUMN cs_ai_run.tool_summaries IS '订单、物流、退款策略等工具调用摘要。';
COMMENT ON COLUMN cs_ai_run.guardrail_summaries IS '生产护栏命中和处理动作摘要。';
COMMENT ON COLUMN cs_ai_run.elapsed_ms IS '从接口接收到生成决策的总耗时。';
COMMENT ON COLUMN cs_ai_run.handoff IS '本次决策是否要求转人工。';

CREATE INDEX IF NOT EXISTS idx_cs_ai_run_conversation_time
    ON cs_ai_run (conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cs_ai_run_handoff_time
    ON cs_ai_run (handoff, created_at DESC);

INSERT INTO cs_ai_run (
    request_id, conversation_id, provider, model, intent, confidence,
    source_summaries, tool_summaries, guardrail_summaries,
    elapsed_ms, handoff, created_at
) VALUES
(
    'REQ-SEED-REFUND', 'CONV-20260710-001', 'DEEPSEEK', 'deepseek-chat', 'REFUND', 0.93000,
    '["KB-REFUND-001@v2026.06 退款与售后规则 score=0.910"]'::jsonb,
    '["order-api：订单 SO202606210004，状态 REFUNDING，金额 5600.00"]'::jsonb,
    '["HALLUCINATION_RISK：高价值退款需要人工确认仓库拦截"]'::jsonb,
    1380, TRUE, '2026-07-10 09:53:00.000000'
),
(
    'REQ-SEED-LOGISTICS', 'CONV-20260710-002', 'DEEPSEEK', 'deepseek-chat', 'LOGISTICS_QUERY', 0.89000,
    '["KB-SHIPPING-001@v2026.06 物流查询规则 score=0.870"]'::jsonb,
    '["logistics-api：顺丰 SF202606210002，运输中"]'::jsonb,
    '["KNOWLEDGE_VERSION_DRIFT：已记录引用版本"]'::jsonb,
    920, FALSE, '2026-07-10 09:29:00.000000'
),
(
    'REQ-SEED-INVOICE', 'CONV-20260710-003', 'OLLAMA', 'qwen2.5:7b', 'INVOICE', 0.84000,
    '["KB-INVOICE-001@v2026.06 发票处理规则 score=0.900"]'::jsonb,
    '[]'::jsonb,
    '["KNOWLEDGE_VERSION_DRIFT：已记录引用版本"]'::jsonb,
    640, FALSE, '2026-07-10 08:52:00.000000'
)
ON CONFLICT (request_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS cs_ticket (
    ticket_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    question TEXT NOT NULL,
    answer_draft TEXT NOT NULL,
    intent VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    owner VARCHAR(128) NOT NULL DEFAULT '',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    resolved_at TIMESTAMP(6),
    duplicate_count INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE cs_ticket IS '客服工单表：记录 AI 转人工后的工单主数据和处理状态。';
COMMENT ON COLUMN cs_ticket.ticket_id IS '工单号，业务主键，例如 CS-20260622-XXXXXXXX。';
COMMENT ON COLUMN cs_ticket.customer_id IS '客户唯一标识。';
COMMENT ON COLUMN cs_ticket.session_id IS '会话 ID，用于关联一次客服上下文。';
COMMENT ON COLUMN cs_ticket.question IS '用户原始问题。';
COMMENT ON COLUMN cs_ticket.answer_draft IS 'AI 生成的回复草稿，供人工处理时参考。';
COMMENT ON COLUMN cs_ticket.intent IS '识别出的客服意图，例如 REFUND、LOGISTICS_QUERY。';
COMMENT ON COLUMN cs_ticket.status IS '工单状态，例如 NEW、ASSIGNED、WAITING_CUSTOMER、RESOLVED。';
COMMENT ON COLUMN cs_ticket.priority IS '工单优先级，例如 LOW、NORMAL、HIGH、URGENT。';
COMMENT ON COLUMN cs_ticket.owner IS '当前处理人，未分配时为空字符串。';
COMMENT ON COLUMN cs_ticket.idempotency_key IS '工单幂等键，用于同一客户同一问题重复触发时复用工单。';
COMMENT ON COLUMN cs_ticket.created_at IS '工单创建时间。';
COMMENT ON COLUMN cs_ticket.updated_at IS '工单最近更新时间。';
COMMENT ON COLUMN cs_ticket.resolved_at IS '工单解决时间，未解决时为空。';
COMMENT ON COLUMN cs_ticket.duplicate_count IS '重复命中次数，用于观察重复咨询或重复建单情况。';

CREATE INDEX IF NOT EXISTS idx_cs_ticket_customer_status ON cs_ticket (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_cs_ticket_status_updated ON cs_ticket (status, updated_at);

CREATE TABLE IF NOT EXISTS cs_ticket_timeline (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ticket_id VARCHAR(64) NOT NULL REFERENCES cs_ticket (ticket_id) ON DELETE CASCADE,
    sequence_no INT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_operator VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    UNIQUE (ticket_id, sequence_no)
);

COMMENT ON TABLE cs_ticket_timeline IS '工单时间线表：记录工单创建、分配、备注、关闭和重复命中等事件。';
COMMENT ON COLUMN cs_ticket_timeline.id IS '时间线自增主键。';
COMMENT ON COLUMN cs_ticket_timeline.ticket_id IS '所属工单号，关联 cs_ticket.ticket_id。';
COMMENT ON COLUMN cs_ticket_timeline.sequence_no IS '工单内事件序号，用于保持时间线展示顺序。';
COMMENT ON COLUMN cs_ticket_timeline.occurred_at IS '事件发生时间。';
COMMENT ON COLUMN cs_ticket_timeline.event_type IS '事件类型，例如 CREATED、ASSIGNED、NOTE_ADDED、RESOLVED。';
COMMENT ON COLUMN cs_ticket_timeline.event_operator IS '事件操作人或系统角色。';
COMMENT ON COLUMN cs_ticket_timeline.message IS '事件说明文本。';

CREATE INDEX IF NOT EXISTS idx_cs_ticket_timeline_ticket_time ON cs_ticket_timeline (ticket_id, occurred_at);

CREATE TABLE IF NOT EXISTS cs_knowledge_document (
    document_id VARCHAR(64) PRIMARY KEY,
    version VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    operator VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

COMMENT ON TABLE cs_knowledge_document IS '知识库文档表：保存客服知识条目的正文、版本、来源和发布状态。';
COMMENT ON COLUMN cs_knowledge_document.document_id IS '知识文档 ID，业务主键，例如 KB-REFUND-001。';
COMMENT ON COLUMN cs_knowledge_document.version IS '知识版本号，用于回答溯源和版本复盘。';
COMMENT ON COLUMN cs_knowledge_document.title IS '知识标题。';
COMMENT ON COLUMN cs_knowledge_document.content IS '知识正文原文。';
COMMENT ON COLUMN cs_knowledge_document.status IS '知识状态，例如 DRAFT、REVIEWING、PUBLISHED、DISABLED。';
COMMENT ON COLUMN cs_knowledge_document.source_type IS '知识来源类型，例如 BUILT_IN、MANUAL。';
COMMENT ON COLUMN cs_knowledge_document.source_name IS '知识来源名称，例如 course-seed、售后 SOP。';
COMMENT ON COLUMN cs_knowledge_document.operator IS '最近一次导入、提交、发布或禁用该知识的操作人。';
COMMENT ON COLUMN cs_knowledge_document.created_at IS '知识创建时间。';
COMMENT ON COLUMN cs_knowledge_document.updated_at IS '知识最近更新时间。';

CREATE INDEX IF NOT EXISTS idx_cs_knowledge_document_status_updated ON cs_knowledge_document (status, updated_at);
CREATE INDEX IF NOT EXISTS idx_cs_knowledge_document_source ON cs_knowledge_document (source_type, source_name);

CREATE TABLE IF NOT EXISTS cs_knowledge_chunk (
    chunk_id VARCHAR(128) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES cs_knowledge_document (document_id) ON DELETE CASCADE,
    version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    chunk_index INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    chunk_text TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    embedding_model VARCHAR(128) NOT NULL DEFAULT '',
    embedding_dim INT,
    embedding vector(64),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE (document_id, chunk_index)
);

COMMENT ON TABLE cs_knowledge_chunk IS '知识分块表：保存已发布或待同步知识的分块文本和向量，用于 RAG 检索。';
COMMENT ON COLUMN cs_knowledge_chunk.chunk_id IS '知识分块 ID，业务主键。';
COMMENT ON COLUMN cs_knowledge_chunk.document_id IS '所属知识文档 ID，关联 cs_knowledge_document.document_id。';
COMMENT ON COLUMN cs_knowledge_chunk.version IS '分块对应的知识版本号。';
COMMENT ON COLUMN cs_knowledge_chunk.status IS '分块状态，通常与知识文档发布状态保持一致。';
COMMENT ON COLUMN cs_knowledge_chunk.chunk_index IS '文档内分块序号，从 0 开始。';
COMMENT ON COLUMN cs_knowledge_chunk.title IS '分块所属知识标题。';
COMMENT ON COLUMN cs_knowledge_chunk.chunk_text IS '分块正文文本。';
COMMENT ON COLUMN cs_knowledge_chunk.content_hash IS '分块内容哈希，用于检测内容变化和同步幂等。';
COMMENT ON COLUMN cs_knowledge_chunk.embedding_status IS '向量同步状态，例如 PENDING、SYNCED、FAILED、DELETED。';
COMMENT ON COLUMN cs_knowledge_chunk.embedding_model IS '生成该分块向量的 embedding 模型名称。';
COMMENT ON COLUMN cs_knowledge_chunk.embedding_dim IS '向量维度。';
COMMENT ON COLUMN cs_knowledge_chunk.embedding IS 'pgvector 向量字段，用于相似度检索。';
COMMENT ON COLUMN cs_knowledge_chunk.created_at IS '分块创建时间。';
COMMENT ON COLUMN cs_knowledge_chunk.updated_at IS '分块最近更新时间。';

CREATE INDEX IF NOT EXISTS idx_cs_knowledge_chunk_document_status ON cs_knowledge_chunk (document_id, status);
CREATE INDEX IF NOT EXISTS idx_cs_knowledge_chunk_embedding_status ON cs_knowledge_chunk (embedding_status, updated_at);
CREATE INDEX IF NOT EXISTS idx_cs_knowledge_chunk_text_gin
    ON cs_knowledge_chunk USING gin (to_tsvector('simple', title || ' ' || chunk_text));
CREATE INDEX IF NOT EXISTS idx_cs_knowledge_chunk_embedding_hnsw
    ON cs_knowledge_chunk USING hnsw (embedding vector_cosine_ops)
    WHERE status = 'PUBLISHED' AND embedding IS NOT NULL;

CREATE TABLE IF NOT EXISTS cs_knowledge_vector_sync (
    chunk_id VARCHAR(128) PRIMARY KEY REFERENCES cs_knowledge_chunk (chunk_id) ON DELETE CASCADE,
    sync_status VARCHAR(32) NOT NULL,
    last_error TEXT,
    synced_at TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL
);

COMMENT ON TABLE cs_knowledge_vector_sync IS '知识向量同步表：记录每个知识分块向量化同步状态和失败原因。';
COMMENT ON COLUMN cs_knowledge_vector_sync.chunk_id IS '知识分块 ID，关联 cs_knowledge_chunk.chunk_id。';
COMMENT ON COLUMN cs_knowledge_vector_sync.sync_status IS '同步状态，例如 PENDING、SYNCED、FAILED、DELETED。';
COMMENT ON COLUMN cs_knowledge_vector_sync.last_error IS '最近一次同步失败原因，成功时可为空。';
COMMENT ON COLUMN cs_knowledge_vector_sync.synced_at IS '最近一次成功同步时间。';
COMMENT ON COLUMN cs_knowledge_vector_sync.updated_at IS '同步记录最近更新时间。';

CREATE INDEX IF NOT EXISTS idx_cs_knowledge_vector_sync_status ON cs_knowledge_vector_sync (sync_status, updated_at);

INSERT INTO cs_knowledge_document (
    document_id, version, title, content, status, source_type, source_name,
    operator, created_at, updated_at
) VALUES
(
    'KB-REFUND-001',
    'v2026.06',
    '退款与售后规则',
    '普通订单在签收后 7 天内可以申请无理由退款，生鲜、定制和虚拟商品除外。
VIP 客户的退款审核 SLA 为 2 小时，企业客户可以走客户成功专属通道。
如果订单已经进入仓库拣货完成状态，系统只能先创建售后工单，由人工确认是否拦截发货。',
    'PUBLISHED',
    'BUILT_IN',
    'course-seed',
    'system',
    '2026-06-22 00:00:00.000000',
    '2026-06-22 00:00:00.000000'
),
(
    'KB-SHIPPING-001',
    'v2026.06',
    '物流查询规则',
    '物流状态以承运商回传为准。超过 24 小时没有更新时，先查询运单轨迹，再判断是否创建物流催办工单。
高价值企业订单需要同步客户成功经理，避免只给出模板化回复。',
    'PUBLISHED',
    'BUILT_IN',
    'course-seed',
    'system',
    '2026-06-22 00:00:00.000000',
    '2026-06-22 00:00:00.000000'
),
(
    'KB-INVOICE-001',
    'v2026.06',
    '发票处理规则',
    '电子发票通常在支付完成后 10 分钟内生成。抬头错误可以在开票后 24 小时内申请重开。
企业客户需要专票时，必须校验税号、开户行和注册地址。',
    'PUBLISHED',
    'BUILT_IN',
    'course-seed',
    'system',
    '2026-06-22 00:00:00.000000',
    '2026-06-22 00:00:00.000000'
),
(
    'KB-AI-GUARDRAIL-001',
    'v2026.06',
    '客服回复安全边界',
    '回复必须优先依据知识库和业务系统查询结果。不确定时不要编造政策、不要承诺赔付金额，应该创建工单并说明需要人工确认。',
    'PUBLISHED',
    'BUILT_IN',
    'course-seed',
    'system',
    '2026-06-22 00:00:00.000000',
    '2026-06-22 00:00:00.000000'
)
ON CONFLICT (document_id) DO NOTHING;


-- 工单种子数据与会话种子一一关联，让下载代码后可以直接演示跨团队协作和 SLA 风险。
INSERT INTO cs_ticket (
    ticket_id, customer_id, session_id, question, answer_draft, intent,
    status, priority, owner, idempotency_key,
    created_at, updated_at, resolved_at, duplicate_count
) VALUES
(
    'CS-20260710-0001', 'C10001', 'CONV-20260710-001',
    '高价值活动物料已经进入仓库，客户要求取消并退款。',
    '需要仓库确认是否拦截发货，再由财务确认退款路径。',
    'REFUND', 'NEW', 'URGENT', '', 'seed:warehouse-interception',
    CURRENT_TIMESTAMP - INTERVAL '55 minutes',
    CURRENT_TIMESTAMP - INTERVAL '54 minutes',
    NULL, 0
),
(
    'CS-20260710-0002', 'C10002', 'CONV-20260710-002',
    '物流超过 24 小时没有更新，需要向承运商发起催查。',
    '建议先确认运单最新轨迹，再决定是否升级物流异常。',
    'LOGISTICS_QUERY', 'ASSIGNED', 'HIGH', 'support-zhangmin', 'seed:logistics-delay',
    CURRENT_TIMESTAMP - INTERVAL '95 minutes',
    CURRENT_TIMESTAMP - INTERVAL '80 minutes',
    NULL, 0
),
(
    'CS-20260710-0003', 'C10003', 'CONV-20260710-003',
    '客户补交企业抬头和税号后，需要重新开具电子发票。',
    '等待客户提供完整开票资料。',
    'INVOICE', 'ASSIGNED', 'NORMAL', 'support-lijie', 'seed:invoice-review',
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    CURRENT_TIMESTAMP - INTERVAL '70 minutes',
    NULL, 0
),
(
    'CS-20260710-0004', 'C10003', 'CONV-20260710-005',
    '会员积分未到账。',
    '核对积分流水后补发。',
    'FAQ', 'RESOLVED', 'LOW', 'support-lijie', 'seed:points-reissue',
    CURRENT_TIMESTAMP - INTERVAL '23 hours',
    CURRENT_TIMESTAMP - INTERVAL '20 hours',
    CURRENT_TIMESTAMP - INTERVAL '20 hours',
    0
)
ON CONFLICT (ticket_id) DO NOTHING;

INSERT INTO cs_ticket_timeline (
    ticket_id, sequence_no, occurred_at, event_type, event_operator, message
) VALUES
('CS-20260710-0001', 1, CURRENT_TIMESTAMP - INTERVAL '55 minutes', 'CREATED', 'ai-orchestrator', 'AI 已创建待处理工单。'),
('CS-20260710-0001', 2, CURRENT_TIMESTAMP - INTERVAL '54 minutes', 'NOTE_ADDED', 'ai-orchestrator', '高价值订单触发人工审批护栏。'),
('CS-20260710-0002', 1, CURRENT_TIMESTAMP - INTERVAL '95 minutes', 'CREATED', 'ai-orchestrator', 'AI 已创建待处理工单。'),
('CS-20260710-0002', 2, CURRENT_TIMESTAMP - INTERVAL '80 minutes', 'ASSIGNED', 'support-zhangmin', '工单已分配给 support-zhangmin。'),
('CS-20260710-0003', 1, CURRENT_TIMESTAMP - INTERVAL '2 hours', 'CREATED', 'ai-orchestrator', 'AI 已创建待处理工单。'),
('CS-20260710-0003', 2, CURRENT_TIMESTAMP - INTERVAL '110 minutes', 'ASSIGNED', 'support-lijie', '工单已分配给 support-lijie。'),
('CS-20260710-0003', 3, CURRENT_TIMESTAMP - INTERVAL '70 minutes', 'NOTE_ADDED', 'support-lijie', '已向客户索取税号与企业抬头。'),
('CS-20260710-0004', 1, CURRENT_TIMESTAMP - INTERVAL '23 hours', 'CREATED', 'ai-orchestrator', 'AI 已创建待处理工单。'),
('CS-20260710-0004', 2, CURRENT_TIMESTAMP - INTERVAL '22 hours', 'ASSIGNED', 'support-lijie', '工单已分配给 support-lijie。'),
('CS-20260710-0004', 3, CURRENT_TIMESTAMP - INTERVAL '20 hours', 'RESOLVED', 'support-lijie', '积分补发成功，客户已确认。')
ON CONFLICT (ticket_id, sequence_no) DO NOTHING;
