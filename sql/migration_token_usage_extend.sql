-- 扩展 Token 用量表：关联会话/工作流/消息，区分 chat / embedding
ALTER TABLE ai_token_usage
    ADD COLUMN session_id BIGINT NULL COMMENT '会话ID' AFTER agent_id,
    ADD COLUMN workflow_id BIGINT NULL COMMENT '工作流ID' AFTER session_id,
    ADD COLUMN message_id BIGINT NULL COMMENT '助手消息ID' AFTER workflow_id,
    ADD COLUMN usage_type VARCHAR(32) DEFAULT 'chat' COMMENT 'chat/embedding' AFTER message_id,
    ADD KEY idx_session_id (session_id),
    ADD KEY idx_workflow_id (workflow_id);
