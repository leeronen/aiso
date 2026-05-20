package com.aios.platform.ai.service;

import com.aios.platform.ai.entity.AiTokenUsage;
import com.aios.platform.ai.mapper.AiTokenUsageMapper;
import com.aios.platform.runtime.LlmTokenUsage;
import com.aios.platform.runtime.TokenUsageContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTokenUsageService {

    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_EMBEDDING = "embedding";

    private final AiTokenUsageMapper usageMapper;

    public void record(
            TokenUsageContext ctx,
            Long agentId,
            String modelCode,
            String usageType,
            LlmTokenUsage usage) {
        if (usage == null || usage.totalTokens() <= 0) {
            return;
        }
        AiTokenUsage row = new AiTokenUsage();
        if (ctx != null) {
            row.setUserId(ctx.getUserId());
            row.setSessionId(ctx.getSessionId());
            row.setWorkflowId(ctx.getWorkflowId());
            row.setMessageId(ctx.getMessageId());
        }
        row.setAgentId(agentId);
        row.setUsageType(usageType != null ? usageType : TYPE_CHAT);
        row.setModelCode(modelCode);
        row.setPromptTokens(usage.promptTokens());
        row.setCompletionTokens(usage.completionTokens());
        row.setTotalTokens(usage.totalTokens());
        usageMapper.insert(row);
    }

    public void recordBatch(
            TokenUsageContext ctx, List<com.aios.platform.runtime.dto.PendingTokenUsageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (var rec : records) {
            record(ctx, rec.getAgentId(), rec.getModelCode(), TYPE_CHAT, rec.getUsage());
        }
    }
}
