package com.aios.platform.runtime.dto;

import lombok.Data;

@Data
public class AgentStepResult {

    private String nodeKey;
    private String nodeLabel;
    private Long agentId;
    private String agentName;
    private String modelCode;
    private long elapsedMs;
    private String output;
    private String error;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
