package com.aios.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LlmTokenUsageTest {

    @Test
    void sum_addsAllFields() {
        LlmTokenUsage a = new LlmTokenUsage(10, 20, 30);
        LlmTokenUsage b = new LlmTokenUsage(1, 2, 3);
        LlmTokenUsage c = LlmTokenUsage.sum(a, b);
        assertEquals(11, c.promptTokens());
        assertEquals(22, c.completionTokens());
        assertEquals(33, c.totalTokens());
    }

    @Test
    void sum_handlesNullAsEmpty() {
        LlmTokenUsage c = LlmTokenUsage.sum(null, new LlmTokenUsage(5, 5, 10));
        assertEquals(5, c.promptTokens());
        assertEquals(10, c.totalTokens());
    }
}
