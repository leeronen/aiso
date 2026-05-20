-- Agent 增强：记忆/工具模式配置、知识库/MCP/Skill 关联
-- mysql -u root -p aios < sql/migration_agent_config.sql

CREATE TABLE IF NOT EXISTS sys_config_option (
    option_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_type VARCHAR(50) NOT NULL COMMENT '配置类型 agent_memory_mode/agent_tool_mode',
    config_code VARCHAR(50) NOT NULL COMMENT '配置编码',
    config_label VARCHAR(100) NOT NULL COMMENT '显示名称',
    description VARCHAR(500) COMMENT '说明',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_type_code (config_type, config_code),
    KEY idx_config_type (config_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统可配置选项';

ALTER TABLE ai_agent
    ADD COLUMN memory_mode VARCHAR(50) DEFAULT 'session' COMMENT '记忆模式编码' AFTER thinking_mode,
    ADD COLUMN tool_mode VARCHAR(50) DEFAULT 'auto' COMMENT '工具选择模式编码' AFTER memory_mode;

UPDATE ai_agent SET memory_mode = IF(IFNULL(memory_enabled, 1) = 0, 'none', 'session') WHERE memory_mode IS NULL OR memory_mode = '';
UPDATE ai_agent SET tool_mode = IF(IFNULL(tool_enabled, 1) = 0, 'none', 'auto') WHERE tool_mode IS NULL OR tool_mode = '';

CREATE TABLE IF NOT EXISTS ai_agent_knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_agent_kb (agent_id, knowledge_base_id),
    KEY idx_knowledge_base_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent-知识库关联';

CREATE TABLE IF NOT EXISTS ai_agent_mcp_server (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    mcp_server_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_agent_mcp (agent_id, mcp_server_id),
    KEY idx_mcp_server_id (mcp_server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent-MCP关联';

CREATE TABLE IF NOT EXISTS ai_agent_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_agent_skill (agent_id, skill_id),
    KEY idx_skill_id (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent-Skill关联';

INSERT IGNORE INTO sys_config_option (config_type, config_code, config_label, description, sort_order, status) VALUES
('agent_memory_mode', 'none', '关闭记忆', '不启用任何记忆', 1, 1),
('agent_memory_mode', 'session', '会话记忆', '仅当前会话上下文', 2, 1),
('agent_memory_mode', 'long_term', '长期记忆', '跨会话持久化记忆', 3, 1),
('agent_memory_mode', 'summary', '摘要记忆', '对历史对话做摘要存储', 4, 1),
('agent_tool_mode', 'none', '关闭工具', '不调用任何工具', 1, 1),
('agent_tool_mode', 'auto', '自动选择', '由 Agent 自动挑选工具', 2, 1),
('agent_tool_mode', 'manual', '手动选择', '仅使用已绑定的 MCP/Skill', 3, 1),
('agent_tool_mode', 'mcp_only', '仅 MCP', '只使用 MCP 工具', 4, 1),
('agent_tool_mode', 'skill_only', '仅 Skill', '只使用 Skill', 5, 1);
