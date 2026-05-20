-- 工作流版本历史：每次保存自动递增版本并留存快照
-- mysql -h127.0.0.1 -P33061 -uaios -paios123 aios < sql/migration_workflow_version.sql

USE aios;

ALTER TABLE ai_workflow
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '当前版本号(递增)' AFTER version;

UPDATE ai_workflow SET version_no = 1 WHERE version_no IS NULL OR version_no < 1;

CREATE TABLE IF NOT EXISTS ai_workflow_version (
    version_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本记录ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    version_no INT NOT NULL COMMENT '版本号',
    workflow_name VARCHAR(100) NOT NULL COMMENT '名称快照',
    description TEXT COMMENT '描述快照',
    input_type VARCHAR(50) DEFAULT 'object',
    output_type VARCHAR(50) DEFAULT 'object',
    input_schema TEXT,
    output_schema TEXT,
    graph_json LONGTEXT COMMENT '画布 JSON',
    dsl_json LONGTEXT COMMENT 'DSL 快照',
    steps_json TEXT COMMENT 'Agent 步骤 JSON',
    change_summary VARCHAR(500) COMMENT '变更说明',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_workflow_version (workflow_id, version_no),
    KEY idx_workflow_id (workflow_id),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流版本历史';
