-- 工作流：入参/出参 schema + 多 Agent 节点
-- mysql -h127.0.0.1 -P33061 -uaios -paios123 aios < sql/migration_workflow.sql

USE aios;

ALTER TABLE ai_workflow
    ADD COLUMN input_type VARCHAR(50) DEFAULT 'object' COMMENT '入参类型' AFTER description,
    ADD COLUMN output_type VARCHAR(50) DEFAULT 'object' COMMENT '出参类型' AFTER input_type,
    ADD COLUMN input_schema TEXT COMMENT '入参 JSON Schema' AFTER output_type,
    ADD COLUMN output_schema TEXT COMMENT '出参 JSON Schema' AFTER input_schema,
    ADD COLUMN execution_mode VARCHAR(32) DEFAULT 'sequential' COMMENT '执行模式 sequential|parallel' AFTER output_schema;

CREATE TABLE IF NOT EXISTS ai_workflow_agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '执行顺序',
    node_key VARCHAR(64) NOT NULL COMMENT '节点标识',
    node_label VARCHAR(100) COMMENT '节点显示名',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_workflow_agent (workflow_id, agent_id),
    UNIQUE KEY uk_workflow_node (workflow_id, node_key),
    KEY idx_workflow_id (workflow_id),
    KEY idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流-Agent 关联';
