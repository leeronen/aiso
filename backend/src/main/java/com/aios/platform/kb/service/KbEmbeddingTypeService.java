package com.aios.platform.kb.service;

import com.aios.platform.common.dto.StringSelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.entity.KbEmbeddingType;
import com.aios.platform.kb.mapper.KbEmbeddingTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KbEmbeddingTypeService {

    private final KbEmbeddingTypeMapper embeddingTypeMapper;

    public List<StringSelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<KbEmbeddingType> q = new LambdaQueryWrapper<>();
        q.eq(KbEmbeddingType::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(KbEmbeddingType::getTypeName, keyword)
                    .or()
                    .like(KbEmbeddingType::getTypeCode, keyword)
                    .or()
                    .like(KbEmbeddingType::getDescription, keyword));
        }
        q.orderByAsc(KbEmbeddingType::getSortOrder).orderByAsc(KbEmbeddingType::getEmbeddingTypeId).last("LIMIT " + size);
        return embeddingTypeMapper.selectList(q).stream()
                .map(t -> new StringSelectOptionVO(t.getTypeCode(), t.getTypeName(), t.getDescription()))
                .toList();
    }

    public String labelOf(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return null;
        }
        KbEmbeddingType row = embeddingTypeMapper.selectOne(
                new LambdaQueryWrapper<KbEmbeddingType>().eq(KbEmbeddingType::getTypeCode, typeCode).last("LIMIT 1"));
        return row != null ? row.getTypeName() : typeCode;
    }

    public void validateCode(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            throw new BusinessException("请选择 Embedding 类型");
        }
        Long count = embeddingTypeMapper.selectCount(
                new LambdaQueryWrapper<KbEmbeddingType>()
                        .eq(KbEmbeddingType::getTypeCode, typeCode)
                        .eq(KbEmbeddingType::getStatus, 1));
        if (count == 0) {
            throw new BusinessException("Embedding 类型无效: " + typeCode);
        }
    }
}
