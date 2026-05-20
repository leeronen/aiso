package com.aios.platform.ai.dto;

import java.util.List;
import lombok.Data;

@Data
public class AiWorkflowSaveRequest {

    private Long workflowId;
    private String workflowName;
    private String description;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Integer status;
    private List<WorkflowStepDTO> steps;
    /** 可视化画布 JSON：nodes + edges（React Flow） */
    private String graphJson;
}
