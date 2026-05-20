package com.aios.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TokenEstimatorTest {

    @Test
    void estimateText_blankReturnsZero() {
        assertEquals(0, TokenEstimator.estimateText(null));
        assertEquals(0, TokenEstimator.estimateText("   "));
    }

    @Test
    void estimateText_nonBlankAtLeastOne() {
        assertEquals(1, TokenEstimator.estimateText("ab"));
        assertEquals(10 / 3, TokenEstimator.estimateText("abcdefghij"));
    }

    @Test
    void estimateFromMessages_sumsPromptAndCompletion() {
        List<LlmMessage> messages =
                List.of(new LlmMessage("system", "123456"), new LlmMessage("user", "abc"));
        LlmTokenUsage usage = TokenEstimator.estimateFromMessages(messages, "reply-content");
        assertEquals(usage.promptTokens() + usage.completionTokens(), usage.totalTokens());
        assertTrue(usage.totalTokens() > 0);
    }
}
