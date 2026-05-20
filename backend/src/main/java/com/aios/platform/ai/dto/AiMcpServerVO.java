package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiMcpServerVO {

    private Long mcpServerId;
    private String serverName;
    private String protocolType;
    private String serverUrl;
    private String authConfig;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Integer status;
    private LocalDateTime createdTime;
}
