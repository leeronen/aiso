package com.aios.platform.kb.support;

import com.aios.platform.kb.entity.KbKnowledgeBase;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 按知识库配置的 Embedding 类型切分文本。 */
public final class EmbeddingChunkStrategy {

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[。！？.!?])\\s*");

    private EmbeddingChunkStrategy() {}

    public static List<String> split(KbKnowledgeBase kb, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String typeCode =
                kb.getEmbeddingTypeCode() != null && !kb.getEmbeddingTypeCode().isBlank()
                        ? kb.getEmbeddingTypeCode().trim().toLowerCase()
                        : "chunk";
        int chunkSize = kb.getChunkSize() != null ? kb.getChunkSize() : 1024;
        int overlap = kb.getOverlapSize() != null ? kb.getOverlapSize() : 128;

        return switch (typeCode) {
            case "document" -> splitDocument(text);
            case "paragraph" -> splitParagraph(text);
            case "sentence" -> splitSentence(text);
            case "title" -> splitTitle(text);
            case "qa_pair" -> splitQaPair(text);
            default -> TextChunkSplitter.split(text, chunkSize, overlap);
        };
    }

    private static List<String> splitDocument(String text) {
        String t = text.replace("\r\n", "\n").trim();
        return t.isEmpty() ? List.of() : List.of(t);
    }

    private static List<String> splitParagraph(String text) {
        String normalized = text.replace("\r\n", "\n").trim();
        String[] blocks = normalized.split("\\n\\s*\\n+");
        List<String> out = new ArrayList<>();
        for (String block : blocks) {
            String piece = block.trim();
            if (!piece.isEmpty()) {
                out.add(piece);
            }
        }
        if (out.isEmpty() && !normalized.isEmpty()) {
            out.add(normalized);
        }
        return out;
    }

    private static List<String> splitSentence(String text) {
        String normalized = text.replace("\r\n", "\n").trim();
        String[] parts = SENTENCE_END.split(normalized);
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String piece = part.trim();
            if (!piece.isEmpty()) {
                out.add(piece);
            }
        }
        if (out.isEmpty() && !normalized.isEmpty()) {
            out.add(normalized);
        }
        return out;
    }

    private static List<String> splitTitle(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.replace("\r\n", "\n").split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("#")) {
                String title = t.replaceAll("^#+\\s*", "").trim();
                if (!title.isEmpty()) {
                    out.add(title);
                }
            } else if (t.length() <= 120 && !t.endsWith("。") && !t.endsWith(".")) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            return splitParagraph(text);
        }
        return out;
    }

    private static List<String> splitQaPair(String text) {
        String normalized = text.replace("\r\n", "\n").trim();
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : normalized.split("\n")) {
            String t = line.trim();
            if (t.matches("^(Q[:：]|问[:：]|A[:：]|答[:：]).*")) {
                if (!current.isEmpty()) {
                    out.add(current.toString().trim());
                    current.setLength(0);
                }
                current.append(t).append('\n');
            } else if (!t.isEmpty()) {
                current.append(t).append('\n');
            } else if (!current.isEmpty()) {
                out.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            out.add(current.toString().trim());
        }
        if (out.isEmpty()) {
            return splitParagraph(text);
        }
        return out;
    }
}
