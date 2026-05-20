package com.aios.platform.kb.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aios.platform.kb.entity.KbKnowledgeBase;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingChunkStrategyTest {

    @Test
    void documentType_returnsSingleChunk() {
        KbKnowledgeBase kb = kb("document", 512, 0);
        List<String> chunks = EmbeddingChunkStrategy.split(kb, "hello\n\nworld");
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("hello"));
    }

    @Test
    void paragraphType_splitsByBlankLines() {
        KbKnowledgeBase kb = kb("paragraph", 512, 0);
        List<String> chunks = EmbeddingChunkStrategy.split(kb, "第一段。\n\n第二段。");
        assertEquals(2, chunks.size());
    }

    @Test
    void chunkType_usesSizeAndOverlap() {
        KbKnowledgeBase kb = kb("chunk", 300, 50);
        String text = "word ".repeat(200);
        List<String> chunks = EmbeddingChunkStrategy.split(kb, text);
        assertTrue(chunks.size() > 1);
    }

    private static KbKnowledgeBase kb(String type, int size, int overlap) {
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setEmbeddingTypeCode(type);
        kb.setChunkSize(size);
        kb.setOverlapSize(overlap);
        return kb;
    }
}
