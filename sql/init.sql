-- AIOS 核心库表结构（与 PRD 对齐）
-- MySQL 8 / utf8mb4

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========== 系统 RBAC ==========

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_user_id BIGINT DEFAULT 0 COMMENT '创建用户ID',
    updated_user_id BIGINT DEFAULT 0 COMMENT '更新用户ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',

    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status),
    KEY idx_created_time (created_time),
    KEY idx_updated_time (updated_time),
    KEY idx_created_user_id (created_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(100) NOT NULL COMMENT '角色编码',
    description VARCHAR(500) COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_role_code (role_code),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_type VARCHAR(50) COMMENT '权限类型 menu/api/button',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    path VARCHAR(200) COMMENT '路由路径',
    component VARCHAR(200) COMMENT '前端组件',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    permission_code VARCHAR(100) COMMENT '绑定权限编码',
    visible TINYINT DEFAULT 1 COMMENT '是否显示',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_parent_id (parent_id),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- ========== 模型供应商（PRD 汇总表） ==========

CREATE TABLE IF NOT EXISTS ai_model_provider (
    provider_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    provider_code VARCHAR(100) NOT NULL COMMENT '编码 openai/claude/...',
    provider_name VARCHAR(100) NOT NULL COMMENT '名称',
    default_base_url VARCHAR(500) COMMENT '默认BaseURL',
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_provider_code (provider_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型供应商';

CREATE TABLE IF NOT EXISTS ai_model (
    model_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模型ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_code VARCHAR(100) NOT NULL COMMENT '模型编码',
    provider_type VARCHAR(100) COMMENT '模型供应商',
    base_url VARCHAR(500) COMMENT '接口地址',
    api_key TEXT COMMENT 'API Key',
    max_tokens INT DEFAULT 0 COMMENT '最大Token',
    temperature DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度',
    top_p DECIMAL(3,2) DEFAULT 1.00 COMMENT 'TopP',
    support_function_call TINYINT DEFAULT 0 COMMENT '支持Function Call',
    support_vision TINYINT DEFAULT 0 COMMENT '支持视觉',
    support_embedding TINYINT DEFAULT 0 COMMENT '支持Embedding',
    status TINYINT DEFAULT 1 COMMENT '状态',
    remark VARCHAR(1000) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_model_code (model_code),
    KEY idx_provider_type (provider_type),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型表';

CREATE TABLE IF NOT EXISTS ai_agent (
    agent_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Agent ID',
    agent_name VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    description TEXT COMMENT '描述',
    avatar VARCHAR(500) COMMENT '头像',
    model_id BIGINT COMMENT '模型ID',
    system_prompt LONGTEXT COMMENT '系统Prompt',
    welcome_message TEXT COMMENT '欢迎语',
    thinking_mode VARCHAR(50) COMMENT '思考模式',
    memory_mode VARCHAR(50) DEFAULT 'session' COMMENT '记忆模式编码(见sys_config_option)',
    tool_mode VARCHAR(50) DEFAULT 'auto' COMMENT '工具选择模式编码(见sys_config_option)',
    memory_enabled TINYINT DEFAULT 1 COMMENT '开启记忆(兼容)',
    knowledge_enabled TINYINT DEFAULT 1 COMMENT '开启知识库(兼容)',
    tool_enabled TINYINT DEFAULT 1 COMMENT '开启工具(兼容)',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_model_id (model_id),
    KEY idx_status (status),
    KEY idx_thinking_mode (thinking_mode),
    KEY idx_created_time (created_time),
    KEY idx_created_user_id (created_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent表';

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

CREATE TABLE IF NOT EXISTS sys_config_option (
    option_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_type VARCHAR(50) NOT NULL COMMENT '配置类型',
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

CREATE TABLE IF NOT EXISTS ai_workflow (
    workflow_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Workflow ID',
    workflow_name VARCHAR(100) NOT NULL COMMENT '工作流名称',
    description TEXT COMMENT '描述',
    input_type VARCHAR(50) DEFAULT 'object' COMMENT '入参类型',
    output_type VARCHAR(50) DEFAULT 'object' COMMENT '出参类型',
    input_schema TEXT COMMENT '入参 JSON Schema',
    output_schema TEXT COMMENT '出参 JSON Schema',
    execution_mode VARCHAR(32) DEFAULT 'graph' COMMENT '已废弃，执行按画布 DAG',
    dsl_json LONGTEXT COMMENT 'DSL配置',
    status TINYINT DEFAULT 1 COMMENT '状态',
    version VARCHAR(50) DEFAULT 'v1' COMMENT '版本标签',
    version_no INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_status (status),
    KEY idx_created_time (created_time),
    KEY idx_created_user_id (created_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流表';

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

CREATE TABLE IF NOT EXISTS ai_mcp_server (
    mcp_server_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'MCP服务ID',
    server_name VARCHAR(100) NOT NULL COMMENT '服务名称',
    protocol_type VARCHAR(50) COMMENT '协议类型',
    server_url VARCHAR(500) COMMENT '服务地址',
    auth_config JSON COMMENT '认证配置',
    input_model_id BIGINT COMMENT '输入模型ID(可选)',
    output_model_id BIGINT COMMENT '输出模型ID(可选)',
    input_type VARCHAR(50) COMMENT '输入类型 text/json/object/array/tool',
    output_type VARCHAR(50) COMMENT '输出类型',
    input_schema TEXT COMMENT '输入 JSON 模板/Schema',
    output_schema TEXT COMMENT '输出 JSON 模板/Schema',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_protocol_type (protocol_type),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP服务表';

CREATE TABLE IF NOT EXISTS ai_function (
    function_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Function ID',
    function_name VARCHAR(100) NOT NULL COMMENT '功能名称',
    function_type VARCHAR(50) COMMENT '功能类型',
    input_schema JSON COMMENT '输入Schema',
    output_schema JSON COMMENT '输出Schema',
    config_json JSON COMMENT '配置JSON',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_function_type (function_type),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Function表';

CREATE TABLE IF NOT EXISTS ai_skill (
    skill_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Skill ID',
    skill_name VARCHAR(100) NOT NULL COMMENT '名称',
    description TEXT COMMENT '描述',
    prompt_template_id BIGINT COMMENT '关联Prompt模板',
    workflow_id BIGINT COMMENT '关联Workflow',
    mcp_server_id BIGINT COMMENT '关联MCP',
    input_model_id BIGINT COMMENT '输入模型ID(可选)',
    output_model_id BIGINT COMMENT '输出模型ID(可选)',
    input_type VARCHAR(50) COMMENT '输入类型',
    output_type VARCHAR(50) COMMENT '输出类型',
    input_schema TEXT COMMENT '输入 JSON 模板/Schema',
    output_schema TEXT COMMENT '输出 JSON 模板/Schema',
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill表';

CREATE TABLE IF NOT EXISTS kb_embedding_type (
    embedding_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    description TEXT COMMENT '说明',
    granularity VARCHAR(50) COMMENT '粒度',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_type_code (type_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Embedding类型配置';

CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    knowledge_base_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识库ID',
    knowledge_base_name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '描述',
    embedding_type_code VARCHAR(50) DEFAULT 'chunk' COMMENT 'Embedding类型编码',
    embedding_model VARCHAR(100) COMMENT '向量模型(如bge-m3)',
    chunk_size INT DEFAULT 1024 COMMENT 'Chunk大小',
    overlap_size INT DEFAULT 128 COMMENT '重叠大小',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS kb_document (
    document_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    knowledge_base_id BIGINT COMMENT '知识库ID',
    document_name VARCHAR(255) NOT NULL COMMENT '文档名称',
    summary VARCHAR(500) COMMENT '简述',
    source_type VARCHAR(20) DEFAULT 'manual' COMMENT '来源 manual/upload/url',
    content LONGTEXT COMMENT '手动录入正文',
    source_url VARCHAR(500) COMMENT '远程下载地址',
    file_type VARCHAR(50) COMMENT '文件类型',
    file_url VARCHAR(500) COMMENT '文件访问地址',
    file_path VARCHAR(500) COMMENT '本地上传路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小字节',
    parse_status VARCHAR(50) COMMENT '解析状态',
    chunk_count INT DEFAULT 0 COMMENT 'Chunk数量',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_knowledge_base_id (knowledge_base_id),
    KEY idx_parse_status (parse_status),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS kb_chunk (
    chunk_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_index INT DEFAULT 0,
    content LONGTEXT,
    metadata_json JSON,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    KEY idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chunk表';

CREATE TABLE IF NOT EXISTS kb_vector_ref (
    ref_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
    vector_id BIGINT NOT NULL COMMENT 'pgvector.kb_chunk_vector.vector_id',
    document_id BIGINT NOT NULL COMMENT '业务库文档ID',
    chunk_id BIGINT NOT NULL COMMENT '业务库ChunkID',
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库ID',
    vector_store VARCHAR(50) DEFAULT 'pgvector' COMMENT '向量库类型',
    embedding_model VARCHAR(100) COMMENT '向量模型',
    status TINYINT DEFAULT 1 COMMENT '状态',
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

CREATE TABLE IF NOT EXISTS prompt_template (
    template_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL,
    category VARCHAR(100),
    content LONGTEXT,
    variables_json JSON,
    version VARCHAR(50) DEFAULT '1.0.0',
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板';

CREATE TABLE IF NOT EXISTS chat_session (
    session_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT COMMENT '用户ID',
    agent_id BIGINT COMMENT 'Agent ID',
    workflow_id BIGINT COMMENT '工作流ID',
    session_title VARCHAR(255) COMMENT '会话标题',
    session_status VARCHAR(50) DEFAULT 'ACTIVE' COMMENT '会话状态',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_user_id (user_id),
    KEY idx_agent_id (agent_id),
    KEY idx_workflow_id (workflow_id),
    KEY idx_session_status (session_status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS chat_message (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT COMMENT '会话ID',
    role_type VARCHAR(50) COMMENT '角色类型',
    content LONGTEXT COMMENT '消息内容',
    token_count INT DEFAULT 0 COMMENT 'Token数量',
    response_time BIGINT DEFAULT 0 COMMENT '响应时间ms',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,

    KEY idx_session_id (session_id),
    KEY idx_role_type (role_type),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS api_key (
    api_key_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(128) NOT NULL,
    api_secret VARCHAR(128) NOT NULL,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_api_key (api_key),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台API Key';

CREATE TABLE IF NOT EXISTS ai_token_usage (
    usage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    agent_id BIGINT,
    session_id BIGINT COMMENT '会话ID',
    workflow_id BIGINT COMMENT '工作流ID',
    message_id BIGINT COMMENT '助手消息ID',
    usage_type VARCHAR(32) DEFAULT 'chat' COMMENT 'chat/embedding',
    model_code VARCHAR(100),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    cost DECIMAL(12,4) DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_user_id BIGINT DEFAULT 0,
    updated_user_id BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    KEY idx_user_id (user_id),
    KEY idx_session_id (session_id),
    KEY idx_workflow_id (workflow_id),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token用量';

CREATE TABLE IF NOT EXISTS sys_login_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    ip VARCHAR(64),
    user_agent VARCHAR(500),
    success TINYINT DEFAULT 1,
    message VARCHAR(255),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- ========== 种子数据（可按需调整） ==========

INSERT IGNORE INTO ai_model_provider (provider_id, provider_code, provider_name, default_base_url, status) VALUES
(1, 'openai', 'OpenAI', 'https://api.openai.com/v1', 1),
(2, 'claude', 'Anthropic Claude', 'https://api.anthropic.com', 1),
(3, 'gemini', 'Google Gemini', 'https://generativelanguage.googleapis.com', 1),
(4, 'deepseek', 'DeepSeek', 'https://api.deepseek.com', 1),
(5, 'qwen', '通义千问', 'https://dashscope.aliyuncs.com', 1),
(6, 'ollama', 'Ollama', 'http://localhost:11434', 1),
(7, 'azure_openai', 'Azure OpenAI', NULL, 1),
(8, 'openrouter', 'OpenRouter', 'https://openrouter.ai/api/v1', 1),
(9, 'siliconflow', 'SiliconFlow', 'https://api.siliconflow.cn/v1', 1);

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

SET FOREIGN_KEY_CHECKS = 1;
