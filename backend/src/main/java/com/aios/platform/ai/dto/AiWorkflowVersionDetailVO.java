package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class AiWorkflowVersionDetailVO {

    private Long versionId;
    private Long workflowId;
    private Integer versionNo;
    private String versionLabel;
    private String workflowName;
    private String description;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private String graphJson;
    private String dslJson;
    private List<WorkflowStepDTO> steps;
    private String changeSummary;
    private LocalDateTime createdTime;
}
