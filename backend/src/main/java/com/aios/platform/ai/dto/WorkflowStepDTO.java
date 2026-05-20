package com.aios.platform.ai.dto;

import lombok.Data;

@Data
public class WorkflowStepDTO {

    private Long agentId;
    private Integer sortOrder;
    private String nodeKey;
    private String nodeLabel;
}
