package com.aios.platform.kb.service;

import com.aios.platform.kb.dto.KbVectorRefVO;
import com.aios.platform.kb.entity.KbVectorRef;
import com.aios.platform.kb.mapper.KbVectorRefMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KbVectorRefService {

    public static final String STORE_PGVECTOR = "pgvector";

    private final KbVectorRefMapper vectorRefMapper;

    @Transactional
    public void saveOrUpdate(
            long vectorId,
            long documentId,
            long chunkId,
            long knowledgeBaseId,
            String embeddingModel) {
        KbVectorRef existing = vectorRefMapper.selectOne(
                new LambdaQueryWrapper<KbVectorRef>()
                        .eq(KbVectorRef::getChunkId, chunkId)
                        .eq(KbVectorRef::getVectorStore, STORE_PGVECTOR)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setVectorId(vectorId);
            existing.setDocumentId(documentId);
            existing.setKnowledgeBaseId(knowledgeBaseId);
            existing.setEmbeddingModel(embeddingModel);
            existing.setStatus(1);
            vectorRefMapper.updateById(existing);
            return;
        }
        KbVectorRef ref = new KbVectorRef();
        ref.setVectorId(vectorId);
        ref.setDocumentId(documentId);
        ref.setChunkId(chunkId);
        ref.setKnowledgeBaseId(knowledgeBaseId);
        ref.setVectorStore(STORE_PGVECTOR);
        ref.setEmbeddingModel(embeddingModel);
        ref.setStatus(1);
        vectorRefMapper.insert(ref);
    }

    @Transactional
    public void deleteByDocumentId(long documentId) {
        vectorRefMapper.delete(
                new LambdaQueryWrapper<KbVectorRef>().eq(KbVectorRef::getDocumentId, documentId));
    }

    @Transactional
    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        vectorRefMapper.delete(new LambdaQueryWrapper<KbVectorRef>().in(KbVectorRef::getChunkId, chunkIds));
    }

    public List<KbVectorRefVO> listByDocumentId(long documentId) {
        return vectorRefMapper
                .selectList(new LambdaQueryWrapper<KbVectorRef>()
                        .eq(KbVectorRef::getDocumentId, documentId)
                        .orderByAsc(KbVectorRef::getChunkId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public long countByDocumentId(long documentId) {
        return vectorRefMapper.selectCount(
                new LambdaQueryWrapper<KbVectorRef>().eq(KbVectorRef::getDocumentId, documentId));
    }

    private KbVectorRefVO toVo(KbVectorRef ref) {
        KbVectorRefVO vo = new KbVectorRefVO();
        vo.setRefId(ref.getRefId());
        vo.setVectorId(ref.getVectorId());
        vo.setDocumentId(ref.getDocumentId());
        vo.setChunkId(ref.getChunkId());
        vo.setKnowledgeBaseId(ref.getKnowledgeBaseId());
        vo.setVectorStore(ref.getVectorStore());
        vo.setEmbeddingModel(ref.getEmbeddingModel());
        vo.setStatus(ref.getStatus());
        vo.setCreatedTime(ref.getCreatedTime());
        return vo;
    }
}
