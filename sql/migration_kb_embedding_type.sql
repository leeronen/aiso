-- Embedding 类型配置表 + 知识库关联字段
-- mysql -u root -p aios < sql/migration_kb_embedding_type.sql

CREATE TABLE IF NOT EXISTS kb_embedding_type (
    embedding_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    description TEXT COMMENT '说明',
    granularity VARCHAR(50) COMMENT '粒度 document/paragraph/chunk/sentence/title/query/qa/entity/table/multimodal',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_type_code (type_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Embedding类型配置';

ALTER TABLE kb_knowledge_base
    ADD COLUMN embedding_type_code VARCHAR(50) DEFAULT 'chunk' COMMENT 'Embedding类型编码' AFTER description;

UPDATE kb_knowledge_base SET embedding_type_code = 'chunk' WHERE embedding_type_code IS NULL OR embedding_type_code = '';

INSERT IGNORE INTO kb_embedding_type (type_code, type_name, description, granularity, sort_order, status) VALUES
('document', '文档 Embedding', '把整篇文档转成向量。适合粗粒度检索，但容易丢细节。', 'document', 1, 1),
('paragraph', '段落 Embedding', '把一个自然段或一小段内容转成向量。知识库里最常见，适合语义搜索。', 'paragraph', 2, 1),
('chunk', 'Chunk Embedding', '把文档切成固定长度或语义块后分别向量化。RAG 最常用，例如每块 300-1000 tokens。', 'chunk', 3, 1),
('sentence', '句子 Embedding', '把单句转成向量。粒度细，适合 FAQ、问答对、短知识点检索。', 'sentence', 4, 1),
('title', '标题 Embedding', '对文档标题、章节标题、小标题做 embedding。常用于辅助召回、过滤、重排。', 'title', 5, 1),
('query', 'Query Embedding', '把用户问题转成向量，用来和知识库里的文档/段落/chunk 向量做相似度匹配。', 'query', 6, 1),
('qa_pair', '问答对 Embedding', '把 FAQ 的问题、答案，或「问题+答案」整体转成向量。客服知识库很常见。', 'qa', 7, 1),
('entity', '实体 Embedding', '对人名、产品名、公司名、概念、术语等实体做 embedding。适合知识图谱、企业知识库、专有名词检索。', 'entity', 8, 1),
('table', '表格 Embedding', '对表格标题、字段、行、单元格上下文做 embedding。适合财务、运营、报表类知识库。', 'table', 9, 1),
('multimodal', '多模态 Embedding', '图片、截图、图表、音频、视频等转成向量，用于图文混合检索。', 'multimodal', 10, 1);
