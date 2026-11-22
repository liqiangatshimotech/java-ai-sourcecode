-- 已有数据库升级时显式运行；不会删除历史审计。
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED';
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS error_code VARCHAR(80);
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(100) NOT NULL DEFAULT 'legacy';
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS input_tokens BIGINT;
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS output_tokens BIGINT;
COMMENT ON COLUMN cs_ai_run.status IS '运行状态：RUNNING、SUCCEEDED、CACHED、FAILED、REJECTED。';
COMMENT ON COLUMN cs_ai_run.error_code IS '失败或被拒绝时的业务错误码，例如 MODEL_BUSY、MODEL_CALL_FAILED；成功时为空。';
COMMENT ON COLUMN cs_ai_run.prompt_version IS '本次使用的 Prompt 版本；升级前的历史记录为 legacy。';
COMMENT ON COLUMN cs_ai_run.input_tokens IS '模型返回的输入 Token 数；供应商没有返回 usage 时为空，命中缓存时为 0。';
COMMENT ON COLUMN cs_ai_run.output_tokens IS '模型返回的输出 Token 数，取值规则同上。';

-- 小型实战允许切换不同向量模型维度；检索按 model + dimension 过滤。
-- 模型切换必须重建全部 chunk，禁止把不同向量空间的结果放在一起比较。
DROP INDEX IF EXISTS idx_cs_knowledge_chunk_embedding_hnsw;
ALTER TABLE cs_knowledge_chunk ALTER COLUMN embedding TYPE vector;
