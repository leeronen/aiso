package com.aios.platform.runtime;

import java.util.List;

/** API 未返回 usage 时的粗算（便于展示，非计费精度）。 */
public final class TokenEstimator {

    private TokenEstimator() {}

    public static int estimateText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 3);
    }

    public static LlmTokenUsage estimateFromMessages(List<LlmMessage> messages, String completion) {
        int prompt = 0;
        if (messages != null) {
            for (LlmMessage m : messages) {
                prompt += estimateText(m.content());
            }
        }
        int completionTokens = estimateText(completion);
        return new LlmTokenUsage(prompt, completionTokens, prompt + completionTokens);
    }
}
