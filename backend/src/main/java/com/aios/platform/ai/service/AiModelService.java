package com.aios.platform.ai.service;

import com.aios.platform.ai.entity.AiModel;
import com.aios.platform.ai.mapper.AiModelMapper;
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelMapper modelMapper;

    public Page<AiModel> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AiModel> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(
                    w ->
                            w.like(AiModel::getModelName, keyword)
                                    .or()
                                    .like(AiModel::getModelCode, keyword)
                                    .or()
                                    .like(AiModel::getProviderType, keyword));
        }
        q.orderByDesc(AiModel::getCreatedTime);
        return modelMapper.selectPage(new Page<>(current, size), q);
    }

    public AiModel get(Long id) {
        AiModel m = modelMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("模型不存在");
        }
        return m;
    }

    @Transactional
    public Long save(AiModel body) {
        if (body.getModelId() == null) {
            if (body.getTemperature() == null) {
                body.setTemperature(new BigDecimal("0.70"));
            }
            if (body.getTopP() == null) {
                body.setTopP(new BigDecimal("1.00"));
            }
            if (body.getStatus() == null) {
                body.setStatus(1);
            }
            modelMapper.insert(body);
            return body.getModelId();
        }
        AiModel db = get(body.getModelId());
        db.setModelName(body.getModelName());
        db.setModelCode(body.getModelCode());
        db.setProviderType(body.getProviderType());
        db.setBaseUrl(body.getBaseUrl());
        if (body.getApiKey() != null && !body.getApiKey().isBlank()) {
            db.setApiKey(body.getApiKey());
        }
        db.setMaxTokens(body.getMaxTokens());
        db.setTemperature(body.getTemperature());
        db.setTopP(body.getTopP());
        db.setSupportFunctionCall(body.getSupportFunctionCall());
        db.setSupportVision(body.getSupportVision());
        db.setSupportEmbedding(body.getSupportEmbedding());
        db.setStatus(body.getStatus());
        db.setRemark(body.getRemark());
        modelMapper.updateById(db);
        return db.getModelId();
    }

    @Transactional
    public void delete(Long id) {
        modelMapper.deleteById(id);
    }

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AiModel> q = new LambdaQueryWrapper<>();
        q.eq(AiModel::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiModel::getModelName, keyword)
                    .or()
                    .like(AiModel::getModelCode, keyword)
                    .or()
                    .like(AiModel::getProviderType, keyword));
        }
        q.orderByAsc(AiModel::getModelName).last("LIMIT " + size);
        return modelMapper.selectList(q).stream()
                .map(m -> new SelectOptionVO(
                        m.getModelId(),
                        m.getModelName() + (m.getModelCode() != null ? " (" + m.getModelCode() + ")" : "")))
                .toList();
    }
}
