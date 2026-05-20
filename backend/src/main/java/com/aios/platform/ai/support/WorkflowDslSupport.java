package com.aios.platform.ai.support;

import com.aios.platform.ai.dto.WorkflowStepDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;

public final class WorkflowDslSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowDslSupport() {}

    public static String versionLabel(int versionNo) {
        return "v" + Math.max(versionNo, 1);
    }

    public static String buildDsl(
            int versionNo,
            String inputType,
            String outputType,
            String inputSchema,
            String outputSchema,
            List<WorkflowStepDTO> steps,
            String graphJson,
            List<List<String>> executionLayers)
            throws JsonProcessingException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("versionNo", versionNo);
        root.put("version", versionLabel(versionNo));

        ObjectNode input = root.putObject("input");
        input.put("type", inputType != null ? inputType : "object");
        input.put("schema", inputSchema != null ? inputSchema : "{}");

        ObjectNode output = root.putObject("output");
        output.put("type", outputType != null ? outputType : "object");
        output.put("schema", outputSchema != null ? outputSchema : "{}");

        ArrayNode nodes = root.putArray("nodes");
        List<WorkflowStepDTO> ordered =
                steps.stream()
                        .sorted(
                                Comparator.comparing(
                                                (WorkflowStepDTO s) ->
                                                        s.getSortOrder() != null ? s.getSortOrder() : 0)
                                        .thenComparing(s -> s.getNodeKey() != null ? s.getNodeKey() : ""))
                        .toList();
        for (WorkflowStepDTO step : ordered) {
            ObjectNode node = nodes.addObject();
            node.put("key", step.getNodeKey());
            node.put("agentId", step.getAgentId());
            node.put("label", step.getNodeLabel() != null ? step.getNodeLabel() : "");
            node.put("order", step.getSortOrder() != null ? step.getSortOrder() : 0);
        }

        if (executionLayers != null && !executionLayers.isEmpty()) {
            ArrayNode layers = root.putArray("executionLayers");
            for (List<String> layer : executionLayers) {
                ArrayNode arr = layers.addArray();
                for (String key : layer) {
                    arr.add(key);
                }
            }
        }

        if (graphJson != null && !graphJson.isBlank()) {
            root.set("graph", MAPPER.readTree(graphJson));
        }
        return MAPPER.writeValueAsString(root);
    }
}
