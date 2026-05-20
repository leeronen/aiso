package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class AiAgentVO {

    private Long agentId;
    private String agentName;
    private String description;
    private String avatar;
    private Long modelId;
    private String modelName;
    private String systemPrompt;
    private String welcomeMessage;
    private String thinkingMode;
    private String memoryMode;
    private String memoryModeLabel;
    private String toolMode;
    private String toolModeLabel;
    private Integer memoryEnabled;
    private Integer knowledgeEnabled;
    private Integer toolEnabled;
    private Integer status;
    private LocalDateTime createdTime;

    private List<Long> knowledgeBaseIds;
    private List<Long> mcpServerIds;
    private List<Long> skillIds;
    private String knowledgeBaseSummary;
    private String mcpServerSummary;
    private String skillSummary;
}
