-- 业务库：向量库 ID 与文档/Chunk 映射关系（MySQL）
-- mysql -u root -p aios < sql/migration_kb_vector_ref.sql

CREATE TABLE IF NOT EXISTS kb_vector_ref (
    ref_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
    vector_id BIGINT NOT NULL COMMENT 'pgvector.kb_chunk_vector.vector_id',
    document_id BIGINT NOT NULL COMMENT '业务库文档ID kb_document',
    chunk_id BIGINT NOT NULL COMMENT '业务库ChunkID kb_chunk',
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库ID',
    vector_store VARCHAR(50) DEFAULT 'pgvector' COMMENT '向量库类型',
    embedding_model VARCHAR(100) COMMENT '向量模型',
    status TINYINT DEFAULT 1 COMMENT '1有效 0无效',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_chunk_store (chunk_id, vector_store),
    UNIQUE KEY uk_vector_store (vector_id, vector_store),
    KEY idx_document_id (document_id),
    KEY idx_knowledge_base_id (knowledge_base_id),
    KEY idx_vector_id (vector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量库与业务文档映射表';
