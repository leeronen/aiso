package com.aios.platform.runtime;

import com.aios.platform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowDslParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowDslParser() {}

    public static WorkflowExecutionPlan parse(String workflowName, String dslJson) {
        if (dslJson == null || dslJson.isBlank()) {
            throw new BusinessException("工作流 DSL 为空");
        }
        try {
            JsonNode root = MAPPER.readTree(dslJson);
            Map<String, WorkflowExecutionPlan.WorkflowNodeSpec> nodesByKey = new HashMap<>();
            for (JsonNode n : root.get("nodes")) {
                String key = text(n, "key");
                if (key == null || key.isBlank()) {
                    continue;
                }
                long agentId = n.has("agentId") ? n.get("agentId").asLong() : 0L;
                if (agentId <= 0) {
                    throw new BusinessException("工作流节点「" + key + "」未绑定 Agent");
                }
                String label = text(n, "label");
                int order = n.has("order") ? n.get("order").asInt() : 0;
                nodesByKey.put(key, new WorkflowExecutionPlan.WorkflowNodeSpec(key, agentId, label, order));
            }
            if (nodesByKey.isEmpty()) {
                throw new BusinessException("工作流未配置 Agent 节点");
            }

            List<List<String>> layers = new ArrayList<>();
            JsonNode layersNode = root.get("executionLayers");
            if (layersNode != null && layersNode.isArray() && !layersNode.isEmpty()) {
                for (JsonNode layer : layersNode) {
                    if (!layer.isArray()) {
                        continue;
                    }
                    List<String> keys = new ArrayList<>();
                    for (JsonNode k : layer) {
                        String key = k.asText();
                        if (nodesByKey.containsKey(key)) {
                            keys.add(key);
                        }
                    }
                    if (!keys.isEmpty()) {
                        layers.add(keys);
                    }
                }
            }
            if (layers.isEmpty()) {
                List<String> ordered =
                        nodesByKey.values().stream()
                                .sorted(Comparator.comparingInt(WorkflowExecutionPlan.WorkflowNodeSpec::order))
                                .map(WorkflowExecutionPlan.WorkflowNodeSpec::nodeKey)
                                .toList();
                for (String key : ordered) {
                    layers.add(List.of(key));
                }
            }
            return new WorkflowExecutionPlan(workflowName, layers, nodesByKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析工作流 DSL 失败: " + e.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
