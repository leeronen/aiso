package com.aios.platform.runtime.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChatExecutionResult {

    private String reply;
    private Long workflowId;
    private String workflowName;
    private List<AgentStepResult> steps = new ArrayList<>();
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private List<PendingTokenUsageRecord> usageRecords = new ArrayList<>();
}
