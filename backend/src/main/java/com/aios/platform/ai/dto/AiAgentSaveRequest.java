package com.aios.platform.ai.dto;

import java.util.List;
import lombok.Data;

@Data
public class AiAgentSaveRequest {

    private Long agentId;
    private String agentName;
    private String description;
    private String avatar;
    private Long modelId;
    private String systemPrompt;
    private String welcomeMessage;
    private String thinkingMode;
    private String memoryMode;
    private String toolMode;
    private Integer status;

    private List<Long> knowledgeBaseIds;
    private List<Long> mcpServerIds;
    private List<Long> skillIds;
}
