package com.aios.platform.kb.service;

import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.dto.KbBaseTreeNode;
import com.aios.platform.kb.dto.KbKnowledgeBaseVO;
import com.aios.platform.kb.entity.KbKnowledgeBase;
import com.aios.platform.kb.mapper.KbKnowledgeBaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KbKnowledgeBaseService {

    private final KbKnowledgeBaseMapper kbMapper;
    private final KbEmbeddingTypeService embeddingTypeService;

    public Page<KbKnowledgeBaseVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<KbKnowledgeBase> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(KbKnowledgeBase::getKnowledgeBaseName, keyword)
                    .or()
                    .like(KbKnowledgeBase::getDescription, keyword));
        }
        q.orderByDesc(KbKnowledgeBase::getCreatedTime);
        Page<KbKnowledgeBase> raw = kbMapper.selectPage(new Page<>(current, size), q);
        Page<KbKnowledgeBaseVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(this::toVo).toList());
        return out;
    }

    public KbKnowledgeBaseVO getVo(Long id) {
        return toVo(get(id));
    }

    public KbKnowledgeBase get(Long id) {
        KbKnowledgeBase k = kbMapper.selectById(id);
        if (k == null) {
            throw new BusinessException("知识库不存在");
        }
        return k;
    }

    @Transactional
    public Long save(KbKnowledgeBase body) {
        String typeCode = body.getEmbeddingTypeCode() != null ? body.getEmbeddingTypeCode() : "chunk";
        embeddingTypeService.validateCode(typeCode);
        body.setEmbeddingTypeCode(typeCode);

        if (body.getKnowledgeBaseId() == null) {
            if (body.getChunkSize() == null) {
                body.setChunkSize(1024);
            }
            if (body.getOverlapSize() == null) {
                body.setOverlapSize(128);
            }
            if (body.getStatus() == null) {
                body.setStatus(1);
            }
            kbMapper.insert(body);
            return body.getKnowledgeBaseId();
        }
        KbKnowledgeBase db = get(body.getKnowledgeBaseId());
        db.setKnowledgeBaseName(body.getKnowledgeBaseName());
        db.setDescription(body.getDescription());
        db.setEmbeddingTypeCode(body.getEmbeddingTypeCode());
        db.setEmbeddingModel(body.getEmbeddingModel());
        db.setChunkSize(body.getChunkSize());
        db.setOverlapSize(body.getOverlapSize());
        db.setStatus(body.getStatus());
        kbMapper.updateById(db);
        return db.getKnowledgeBaseId();
    }

    @Transactional
    public void delete(Long id) {
        kbMapper.deleteById(id);
    }

    public List<KbBaseTreeNode> buildTree(Map<Long, Long> documentCounts) {
        List<KbKnowledgeBase> kbs = kbMapper.selectList(
                new LambdaQueryWrapper<KbKnowledgeBase>().orderByAsc(KbKnowledgeBase::getKnowledgeBaseName));
        long total = documentCounts.values().stream().mapToLong(Long::longValue).sum();
        List<KbBaseTreeNode> nodes = new ArrayList<>();
        KbBaseTreeNode all = new KbBaseTreeNode();
        all.setKey(0L);
        all.setTitle("全部知识库");
        all.setDocumentCount((int) total);
        nodes.add(all);
        for (KbKnowledgeBase kb : kbs) {
            KbBaseTreeNode n = new KbBaseTreeNode();
            n.setKey(kb.getKnowledgeBaseId());
            int count = documentCounts.getOrDefault(kb.getKnowledgeBaseId(), 0L).intValue();
            n.setTitle(kb.getKnowledgeBaseName() + " (" + count + ")");
            n.setDocumentCount(count);
            nodes.add(n);
        }
        return nodes;
    }

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<KbKnowledgeBase> q = new LambdaQueryWrapper<>();
        q.eq(KbKnowledgeBase::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(KbKnowledgeBase::getKnowledgeBaseName, keyword)
                    .or()
                    .like(KbKnowledgeBase::getDescription, keyword));
        }
        q.orderByAsc(KbKnowledgeBase::getKnowledgeBaseName).last("LIMIT " + size);
        return kbMapper.selectList(q).stream()
                .map(k -> new SelectOptionVO(k.getKnowledgeBaseId(), k.getKnowledgeBaseName()))
                .toList();
    }

    private KbKnowledgeBaseVO toVo(KbKnowledgeBase k) {
        KbKnowledgeBaseVO vo = new KbKnowledgeBaseVO();
        vo.setKnowledgeBaseId(k.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(k.getKnowledgeBaseName());
        vo.setDescription(k.getDescription());
        vo.setEmbeddingTypeCode(k.getEmbeddingTypeCode());
        vo.setEmbeddingTypeName(embeddingTypeService.labelOf(k.getEmbeddingTypeCode()));
        vo.setEmbeddingModel(k.getEmbeddingModel());
        vo.setChunkSize(k.getChunkSize());
        vo.setOverlapSize(k.getOverlapSize());
        vo.setStatus(k.getStatus());
        vo.setCreatedTime(k.getCreatedTime());
        return vo;
    }
}
