package com.aios.platform.ai.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_mcp_server")
public class AiMcpServer extends BaseEntity {

    @TableId(value = "mcp_server_id", type = IdType.AUTO)
    private Long mcpServerId;

    private String serverName;
    private String protocolType;
    private String serverUrl;
    private String authConfig;
    private Long inputModelId;
    private Long outputModelId;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Integer status;
}
