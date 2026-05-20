package com.aios.platform.invoke.dto;

import lombok.Data;

@Data
public class WorkflowIoMetaVO {

    private Long workflowId;
    private String workflowName;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Object exampleInput;
}
