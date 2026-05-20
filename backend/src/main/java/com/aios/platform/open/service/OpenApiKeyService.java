package com.aios.platform.open.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.open.entity.OpenApiKey;
import com.aios.platform.open.mapper.OpenApiKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenApiKeyService {

    private final OpenApiKeyMapper apiKeyMapper;

    public OpenApiKey requireValid(String keyValue) {
        if (keyValue == null || keyValue.isBlank()) {
            throw new BusinessException(401, "缺少 API Key");
        }
        OpenApiKey key =
                apiKeyMapper.selectOne(
                        new LambdaQueryWrapper<OpenApiKey>()
                                .eq(OpenApiKey::getApiKey, keyValue.trim())
                                .last("LIMIT 1"));
        if (key == null) {
            throw new BusinessException(401, "无效的 API Key");
        }
        if (key.getStatus() != null && key.getStatus() == 0) {
            throw new BusinessException(401, "API Key 已停用");
        }
        return key;
    }
}
