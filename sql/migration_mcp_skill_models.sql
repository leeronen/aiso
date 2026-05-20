-- 已有库执行：为 MCP / Skill 增加输入输出模型字段
USE aios;

ALTER TABLE ai_mcp_server
    ADD COLUMN IF NOT EXISTS input_model_id BIGINT COMMENT '输入模型ID' AFTER auth_config,
    ADD COLUMN IF NOT EXISTS output_model_id BIGINT COMMENT '输出模型ID' AFTER input_model_id;

ALTER TABLE ai_skill
    ADD COLUMN IF NOT EXISTS input_model_id BIGINT COMMENT '输入模型ID' AFTER mcp_server_id,
    ADD COLUMN IF NOT EXISTS output_model_id BIGINT COMMENT '输出模型ID' AFTER input_model_id;
