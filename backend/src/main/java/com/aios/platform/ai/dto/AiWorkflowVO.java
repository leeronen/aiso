package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class AiWorkflowVO {

    private Long workflowId;
    private String workflowName;
    private String description;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private String dslJson;
    private String version;
    private Integer versionNo;
    private Integer status;
    private LocalDateTime createdTime;
    private List<WorkflowStepDTO> steps;
    private String graphJson;
    private String agentSummary;
    private Integer agentCount;
}
