-- 文档管理增强：来源类型、简述、正文、本地上传路径
-- mysql -u root -p aios < sql/migration_kb_document.sql

ALTER TABLE kb_document
    ADD COLUMN summary VARCHAR(500) COMMENT '简述' AFTER document_name,
    ADD COLUMN source_type VARCHAR(20) DEFAULT 'manual' COMMENT '来源 manual/upload/url' AFTER summary,
    ADD COLUMN content LONGTEXT COMMENT '手动录入正文' AFTER source_type,
    ADD COLUMN source_url VARCHAR(500) COMMENT '远程下载地址' AFTER content,
    ADD COLUMN file_path VARCHAR(500) COMMENT '本地上传路径' AFTER file_url,
    ADD COLUMN file_size BIGINT DEFAULT 0 COMMENT '文件大小字节' AFTER file_path;

UPDATE kb_document SET source_type = 'url' WHERE source_type IS NULL AND file_url IS NOT NULL AND file_url <> '';
UPDATE kb_document SET source_type = 'manual' WHERE source_type IS NULL OR source_type = '';
