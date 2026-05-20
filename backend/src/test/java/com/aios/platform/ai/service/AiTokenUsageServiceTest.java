package com.aios.platform.ai.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aios.platform.ai.entity.AiTokenUsage;
import com.aios.platform.ai.mapper.AiTokenUsageMapper;
import com.aios.platform.runtime.LlmTokenUsage;
import com.aios.platform.runtime.TokenUsageContext;
import com.aios.platform.runtime.dto.PendingTokenUsageRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiTokenUsageServiceTest {

    @Mock
    private AiTokenUsageMapper usageMapper;

    @InjectMocks
    private AiTokenUsageService service;

    @Test
    void record_skipsZeroTokens() {
        service.record(null, 1L, "gpt-4", AiTokenUsageService.TYPE_CHAT, LlmTokenUsage.empty());
        verify(usageMapper, never()).insert(ArgumentMatchers.<AiTokenUsage>any());
    }

    @Test
    void record_persistsUsageWithContext() {
        TokenUsageContext ctx =
                TokenUsageContext.builder()
                        .userId(10L)
                        .sessionId(20L)
                        .workflowId(30L)
                        .messageId(40L)
                        .build();
        service.record(ctx, 5L, "gpt-4o", AiTokenUsageService.TYPE_CHAT, new LlmTokenUsage(100, 50, 150));

        ArgumentCaptor<AiTokenUsage> cap = ArgumentCaptor.forClass(AiTokenUsage.class);
        verify(usageMapper).insert(cap.capture());
        AiTokenUsage row = cap.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(10L, row.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(20L, row.getSessionId());
        org.junit.jupiter.api.Assertions.assertEquals(150, row.getTotalTokens());
        org.junit.jupiter.api.Assertions.assertEquals("chat", row.getUsageType());
    }

    @Test
    void recordBatch_insertsEachRecord() {
        PendingTokenUsageRecord r1 = new PendingTokenUsageRecord();
        r1.setAgentId(1L);
        r1.setModelCode("m1");
        r1.setUsage(new LlmTokenUsage(1, 2, 3));
        PendingTokenUsageRecord r2 = new PendingTokenUsageRecord();
        r2.setAgentId(2L);
        r2.setModelCode("m2");
        r2.setUsage(new LlmTokenUsage(4, 5, 9));
        service.recordBatch(TokenUsageContext.builder().build(), List.of(r1, r2));
        verify(usageMapper, times(2)).insert(ArgumentMatchers.<AiTokenUsage>any());
    }
}
