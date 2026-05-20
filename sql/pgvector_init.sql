-- pgvector 向量库初始化（PostgreSQL）
-- 数据库: aios_vector

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS kb_chunk_vector (
    vector_id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    embedding_model VARCHAR(100),
    embedding vector(1536) NOT NULL,
    created_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_kb ON kb_chunk_vector (knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_doc ON kb_chunk_vector (document_id);

-- 余弦相似度检索（HNSW）
CREATE INDEX IF NOT EXISTS idx_kb_chunk_vector_embedding_hnsw
    ON kb_chunk_vector USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE kb_chunk_vector IS '知识库 Chunk 向量（pgvector）';
