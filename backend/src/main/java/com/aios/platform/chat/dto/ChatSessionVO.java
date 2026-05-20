package com.aios.platform.chat.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatSessionVO {

    private Long sessionId;
    private Long userId;
    private Long agentId;
    private Long workflowId;
    private String workflowName;
    private String sessionTitle;
    private String sessionStatus;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
