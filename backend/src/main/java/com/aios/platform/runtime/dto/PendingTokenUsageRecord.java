package com.aios.platform.runtime.dto;

import com.aios.platform.runtime.LlmTokenUsage;
import lombok.Data;

@Data
public class PendingTokenUsageRecord {

    private Long agentId;
    private String modelCode;
    private LlmTokenUsage usage;
}
