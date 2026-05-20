package com.aios.platform.kb.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.config.VectorStoreProperties;
import com.aios.platform.kb.dto.KbRagHitVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "aios.vector.enabled", havingValue = "true", matchIfMissing = true)
public class KbVectorStoreService {

    private final JdbcTemplate vectorJdbc;
    private final VectorStoreProperties properties;
    private final EmbeddingService embeddingService;

    public KbVectorStoreService(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbc,
            VectorStoreProperties properties,
            EmbeddingService embeddingService) {
        this.vectorJdbc = vectorJdbc;
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    public long upsert(
            long chunkId,
            long knowledgeBaseId,
            long documentId,
            int chunkIndex,
            String content,
            float[] embedding,
            String embeddingModel) {
        String vectorLiteral = toVectorLiteral(embedding);
        Long vectorId = vectorJdbc.queryForObject(
                """
                INSERT INTO kb_chunk_vector
                    (chunk_id, knowledge_base_id, document_id, chunk_index, content, embedding_model, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector)
                ON CONFLICT (chunk_id) DO UPDATE SET
                    knowledge_base_id = EXCLUDED.knowledge_base_id,
                    document_id = EXCLUDED.document_id,
                    chunk_index = EXCLUDED.chunk_index,
                    content = EXCLUDED.content,
                    embedding_model = EXCLUDED.embedding_model,
                    embedding = EXCLUDED.embedding
                RETURNING vector_id
                """,
                Long.class,
                chunkId,
                knowledgeBaseId,
                documentId,
                chunkIndex,
                content,
                embeddingModel,
                vectorLiteral);
        if (vectorId == null) {
            throw new BusinessException("写入向量库失败");
        }
        return vectorId;
    }

    public void deleteByDocumentId(long documentId) {
        vectorJdbc.update("DELETE FROM kb_chunk_vector WHERE document_id = ?", documentId);
    }

    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", chunkIds.stream().map(id -> "?").toList());
        vectorJdbc.update("DELETE FROM kb_chunk_vector WHERE chunk_id IN (" + placeholders + ")", chunkIds.toArray());
    }

    public List<KbRagHitVO> search(long knowledgeBaseId, String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("检索内容不能为空");
        }
        int limit = Math.min(Math.max(topK, 1), 50);
        float[] queryVec = embeddingService.embedOne(query);
        String vectorLiteral = toVectorLiteral(queryVec);
        return vectorJdbc.query(
                """
                SELECT vector_id, chunk_id, document_id, chunk_index, content,
                       1 - (embedding <=> ?::vector) AS score
                FROM kb_chunk_vector
                WHERE knowledge_base_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                ragHitMapper(),
                vectorLiteral,
                knowledgeBaseId,
                vectorLiteral,
                limit);
    }

    public long countByKnowledgeBase(long knowledgeBaseId) {
        Long c = vectorJdbc.queryForObject(
                "SELECT COUNT(*) FROM kb_chunk_vector WHERE knowledge_base_id = ?", Long.class, knowledgeBaseId);
        return c != null ? c : 0;
    }

    private static RowMapper<KbRagHitVO> ragHitMapper() {
        return (ResultSet rs, int rowNum) -> {
            KbRagHitVO vo = new KbRagHitVO();
            vo.setVectorId(rs.getLong("vector_id"));
            vo.setChunkId(rs.getLong("chunk_id"));
            vo.setDocumentId(rs.getLong("document_id"));
            vo.setChunkIndex(rs.getInt("chunk_index"));
            vo.setContent(rs.getString("content"));
            vo.setScore(rs.getDouble("score"));
            return vo;
        };
    }

    private String toVectorLiteral(float[] embedding) {
        if (embedding.length != properties.getDimensions()) {
            throw new BusinessException(
                    "向量维度不匹配: 期望 " + properties.getDimensions() + " 实际 " + embedding.length);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
