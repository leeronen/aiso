package com.aios.platform.chat.dto;

import lombok.Data;

@Data
public class ChatTokenStatsVO {

    private Long sessionId;
    private int messageTotalTokens;
    private int usageRecordPromptTokens;
    private int usageRecordCompletionTokens;
    private int usageRecordTotalTokens;
    private int llmCallCount;
}
