-- JSON 数组保留窗口内消息顺序；这不是完整 History 表。
CREATE TABLE IF NOT EXISTS ai_chat_memory (
 memory_id VARCHAR(128) PRIMARY KEY,
 messages_json CLOB NOT NULL
);
