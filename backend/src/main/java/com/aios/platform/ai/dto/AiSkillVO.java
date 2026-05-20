package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiSkillVO {

    private Long skillId;
    private String skillName;
    private String description;
    private Long promptTemplateId;
    private Long workflowId;
    private Long mcpServerId;
    private String mcpServerName;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Integer status;
    private LocalDateTime createdTime;
}
