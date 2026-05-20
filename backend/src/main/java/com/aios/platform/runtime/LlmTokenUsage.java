package com.aios.platform.runtime;

/** 大模型 API 返回的用量（OpenAI 兼容 usage 字段）。 */
public record LlmTokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static LlmTokenUsage empty() {
        return new LlmTokenUsage(0, 0, 0);
    }

    public static LlmTokenUsage sum(LlmTokenUsage a, LlmTokenUsage b) {
        if (a == null) {
            a = empty();
        }
        if (b == null) {
            b = empty();
        }
        return new LlmTokenUsage(
                a.promptTokens() + b.promptTokens(),
                a.completionTokens() + b.completionTokens(),
                a.totalTokens() + b.totalTokens());
    }
}
