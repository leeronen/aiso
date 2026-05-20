package com.aios.platform.ai.support;

import com.aios.platform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;

public final class IoSchemaSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final Set<String> ALLOWED_TYPES = Set.of("text", "json", "object", "array", "tool", "message");

    private IoSchemaSupport() {}

    public static void validate(String type, String schemaJson, String label) {
        if (type != null && !type.isBlank() && !ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(label + "类型不支持，可选: text/json/object/array/tool/message");
        }
        if (schemaJson == null || schemaJson.isBlank()) {
            return;
        }
        try {
            MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            throw new BusinessException(label + " JSON 模板格式不正确");
        }
    }
}
