package com.aios.platform.ai.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiWorkflowVersionVO {

    private Long versionId;
    private Long workflowId;
    private Integer versionNo;
    private String versionLabel;
    private String workflowName;
    private String changeSummary;
    private LocalDateTime createdTime;
    private Integer agentCount;
}
