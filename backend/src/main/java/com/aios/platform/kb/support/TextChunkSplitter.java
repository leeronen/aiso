package com.aios.platform.kb.support;

import java.util.ArrayList;
import java.util.List;

public final class TextChunkSplitter {

    private TextChunkSplitter() {}

    public static List<String> split(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int size = Math.max(chunkSize, 256);
        int ov = Math.max(0, Math.min(overlap, size / 2));
        String normalized = text.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + size, normalized.length());
            if (end < normalized.length()) {
                int breakAt = findBreak(normalized, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - ov, start + 1);
        }
        return chunks;
    }

    private static int findBreak(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '.' || c == '!') {
                return i + 1;
            }
        }
        return end;
    }
}
