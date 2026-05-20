package com.aios.platform.invoke.dto;

import com.aios.platform.runtime.dto.AgentStepResult;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class WorkflowInvokeResultVO {

    private Long workflowId;
    private String workflowName;
    private String inputType;
    private String outputType;
    private Object input;
    private Object output;
    private String rawReply;
    private List<AgentStepResult> steps = new ArrayList<>();
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private long elapsedMs;
}
