package com.aios.platform.open.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.open.entity.OpenApiKey;
import com.aios.platform.open.mapper.OpenApiKeyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenApiKeyServiceTest {

    @Mock
    private OpenApiKeyMapper apiKeyMapper;

    @InjectMocks
    private OpenApiKeyService service;

    @Test
    void requireValid_missingKey() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.requireValid("  "));
        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("缺少 API Key"));
    }

    @Test
    void requireValid_unknownKey() {
        when(apiKeyMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.requireValid("bad-key"));
        assertEquals(401, ex.getCode());
    }

    @Test
    void requireValid_disabledKey() {
        OpenApiKey key = new OpenApiKey();
        key.setApiKey("k1");
        key.setStatus(0);
        when(apiKeyMapper.selectOne(any())).thenReturn(key);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.requireValid("k1"));
        assertTrue(ex.getMessage().contains("停用"));
    }

    @Test
    void requireValid_returnsActiveKey() {
        OpenApiKey key = new OpenApiKey();
        key.setApiKeyId(1L);
        key.setApiKey("valid-key");
        key.setStatus(1);
        when(apiKeyMapper.selectOne(any())).thenReturn(key);
        OpenApiKey out = service.requireValid("  valid-key  ");
        assertEquals(1L, out.getApiKeyId());
    }
}
