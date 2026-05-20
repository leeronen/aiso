package com.aios.platform.ai.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aios.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class IoSchemaSupportTest {

    @Test
    void validate_acceptsKnownTypes() {
        assertDoesNotThrow(() -> IoSchemaSupport.validate("object", "{}", "入参"));
        assertDoesNotThrow(() -> IoSchemaSupport.validate("text", null, "出参"));
    }

    @Test
    void validate_rejectsUnknownType() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> IoSchemaSupport.validate("xml", "{}", "入参"));
        assertTrue(ex.getMessage().contains("类型不支持"));
    }

    @Test
    void validate_rejectsInvalidJson() {
        assertThrows(
                BusinessException.class, () -> IoSchemaSupport.validate("json", "{bad", "入参"));
    }
}
