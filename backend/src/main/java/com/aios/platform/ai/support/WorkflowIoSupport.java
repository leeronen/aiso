package com.aios.platform.ai.support;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.runtime.dto.AgentStepResult;
import com.aios.platform.runtime.dto.ChatExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** 工作流入参解析与出参组装。 */
public final class WorkflowIoSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowIoSupport() {}

    public static void validateInput(String inputType, String schemaJson, JsonNode input) {
        String type = normalizeType(inputType);
        if (input == null || input.isNull()) {
            if ("text".equals(type)) {
                throw new BusinessException("入参不能为空");
            }
            throw new BusinessException("请提供 JSON 入参");
        }
        if ("text".equals(type)) {
            if (input.isTextual() && input.asText().isBlank()) {
                throw new BusinessException("入参文本不能为空");
            }
            return;
        }
        if (!input.isObject() && !"array".equals(type)) {
            throw new BusinessException("入参须为 JSON 对象或数组");
        }
        JsonNode schema = parseSchema(schemaJson);
        if (schema == null) {
            return;
        }
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode r : required) {
                String field = r.asText();
                if (!input.has(field) || input.get(field).isNull()) {
                    throw new BusinessException("缺少必填入参: " + field);
                }
            }
        }
    }

    public static String toPromptMessage(String inputType, JsonNode input) {
        String type = normalizeType(inputType);
        if (input == null || input.isNull()) {
            return "";
        }
        if ("text".equals(type)) {
            if (input.isTextual()) {
                return input.asText();
            }
            if (input.isObject()) {
                for (String key : List.of("content", "text", "query", "message")) {
                    if (input.has(key) && !input.get(key).isNull()) {
                        return input.get(key).asText();
                    }
                }
            }
            return input.toString();
        }
        if ("message".equals(type) && input.isObject()) {
            if (input.has("content")) {
                return input.get("content").asText();
            }
        }
        if (input.isObject()) {
            for (String key : List.of("query", "content", "message", "input", "text")) {
                if (input.has(key) && !input.get(key).isNull()) {
                    JsonNode v = input.get(key);
                    if (v.isTextual()) {
                        return v.asText();
                    }
                }
            }
            try {
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(input);
            } catch (Exception e) {
                return input.toString();
            }
        }
        return input.toString();
    }

    public static Object formatOutput(
            String outputType, String schemaJson, ChatExecutionResult execution) {
        String type = normalizeType(outputType);
        String reply = execution.getReply() != null ? execution.getReply() : "";
        JsonNode parsed = tryParseJson(reply);

        return switch (type) {
            case "text" -> Map.of("content", reply);
            case "message" -> Map.of("role", "assistant", "content", reply);
            case "array" -> {
                if (parsed != null && parsed.isArray()) {
                    yield MAPPER.convertValue(parsed, Object.class);
                }
                ArrayNode arr = MAPPER.createArrayNode();
                arr.add(reply);
                yield MAPPER.convertValue(arr, Object.class);
            }
            case "json" -> parsed != null ? MAPPER.convertValue(parsed, Object.class) : Map.of("data", reply);
            default -> buildObjectOutput(execution, reply, parsed, schemaJson);
        };
    }

    private static Object buildObjectOutput(
            ChatExecutionResult execution, String reply, JsonNode parsed, String schemaJson) {
        ObjectNode out = MAPPER.createObjectNode();
        if (parsed != null && parsed.isObject()) {
            out.setAll((ObjectNode) parsed);
        } else {
            out.put("answer", reply);
        }
        if (execution.getSteps() != null && !execution.getSteps().isEmpty()) {
            ArrayNode steps = out.putArray("steps");
            for (AgentStepResult s : execution.getSteps()) {
                ObjectNode o = steps.addObject();
                o.put("nodeKey", s.getNodeKey());
                o.put("agentName", s.getAgentName());
                o.put("output", s.getOutput());
                o.put("elapsedMs", s.getElapsedMs());
                o.put("totalTokens", s.getTotalTokens());
                if (s.getError() != null) {
                    o.put("error", s.getError());
                }
            }
        }
        out.put("workflowId", execution.getWorkflowId());
        out.put("workflowName", execution.getWorkflowName());
        out.put("totalTokens", execution.getTotalTokens());
        applySchemaHints(out, schemaJson);
        return MAPPER.convertValue(out, Object.class);
    }

    private static void applySchemaHints(ObjectNode out, String schemaJson) {
        JsonNode schema = parseSchema(schemaJson);
        if (schema == null || !schema.has("properties")) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = schema.get("properties").fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            if (!out.has(key) && e.getValue().has("default")) {
                out.set(key, e.getValue().get("default"));
            }
        }
    }

    public static JsonNode exampleInput(String inputType, String schemaJson) {
        JsonNode schema = parseSchema(schemaJson);
        if (schema != null && schema.has("example")) {
            return schema.get("example");
        }
        String type = normalizeType(inputType);
        return switch (type) {
            case "text" -> MAPPER.getNodeFactory().textNode("你好");
            case "message" -> {
                ObjectNode o = MAPPER.createObjectNode();
                o.put("role", "user");
                o.put("content", "你好");
                yield o;
            }
            case "array" -> MAPPER.createArrayNode().add("item1");
            default -> buildExampleFromSchema(schema);
        };
    }

    private static ObjectNode buildExampleFromSchema(JsonNode schema) {
        ObjectNode ex = MAPPER.createObjectNode();
        if (schema == null || !schema.has("properties")) {
            ex.put("query", "你好");
            return ex;
        }
        Iterator<Map.Entry<String, JsonNode>> it = schema.get("properties").fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            JsonNode prop = e.getValue();
            if (prop.has("example")) {
                ex.set(key, prop.get("example"));
            } else if (prop.has("default")) {
                ex.set(key, prop.get("default"));
            } else {
                String t = prop.path("type").asText("string");
                ex.put(key, exampleForPropertyType(t));
            }
        }
        return ex;
    }

    private static String exampleForPropertyType(String type) {
        return switch (type) {
            case "integer", "number" -> "0";
            case "boolean" -> "false";
            case "array" -> "[]";
            case "object" -> "{}";
            default -> "";
        };
    }

    private static JsonNode parseSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonNode tryParseJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "object";
        }
        return type.trim().toLowerCase();
    }
}
