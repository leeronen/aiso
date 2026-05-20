-- MCP / Skill：输入输出改为类型 + JSON 模板（已有库执行）
USE aios;

ALTER TABLE ai_mcp_server
    ADD COLUMN input_type VARCHAR(50) COMMENT '输入类型 text/json/object/array/tool' AFTER output_model_id,
    ADD COLUMN output_type VARCHAR(50) COMMENT '输出类型' AFTER input_type,
    ADD COLUMN input_schema TEXT COMMENT '输入 JSON 模板/Schema' AFTER output_type,
    ADD COLUMN output_schema TEXT COMMENT '输出 JSON 模板/Schema' AFTER input_schema;

ALTER TABLE ai_skill
    ADD COLUMN input_type VARCHAR(50) COMMENT '输入类型' AFTER output_model_id,
    ADD COLUMN output_type VARCHAR(50) COMMENT '输出类型' AFTER input_type,
    ADD COLUMN input_schema TEXT COMMENT '输入 JSON 模板/Schema' AFTER output_type,
    ADD COLUMN output_schema TEXT COMMENT '输出 JSON 模板/Schema' AFTER input_schema;
