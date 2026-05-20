-- 会话关联工作流
-- docker exec -i aios-mysql mysql -uaios -paios123 aios < sql/migration_chat_workflow.sql

USE aios;

ALTER TABLE chat_session
    ADD COLUMN workflow_id BIGINT NULL COMMENT '工作流ID' AFTER agent_id,
    ADD KEY idx_workflow_id (workflow_id);
