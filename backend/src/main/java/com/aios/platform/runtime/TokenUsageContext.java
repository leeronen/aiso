package com.aios.platform.runtime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUsageContext {

    private Long userId;
    private Long sessionId;
    private Long workflowId;
    /** 助手消息落库后可回填 */
    private Long messageId;
}
