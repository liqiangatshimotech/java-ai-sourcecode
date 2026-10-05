-- 已有数据库升级时显式运行；不会删除历史审计。
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED';
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS error_code VARCHAR(80);
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(100) NOT NULL DEFAULT 'legacy';
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS input_tokens BIGINT;
ALTER TABLE cs_ai_run ADD COLUMN IF NOT EXISTS output_tokens BIGINT;

-- 小型实战允许切换不同向量模型维度；检索按 model + dimension 过滤。
-- 模型切换必须重建全部 chunk，禁止把不同向量空间的结果放在一起比较。
DROP INDEX IF EXISTS idx_cs_knowledge_chunk_embedding_hnsw;
ALTER TABLE cs_knowledge_chunk ALTER COLUMN embedding TYPE vector;
