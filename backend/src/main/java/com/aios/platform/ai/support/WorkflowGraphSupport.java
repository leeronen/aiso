package com.aios.platform.ai.support;

import com.aios.platform.ai.dto.WorkflowStepDTO;
import com.aios.platform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/** 工作流可视化图：解析 graph JSON、拓扑排序得到 Agent 执行链。 */
public final class WorkflowGraphSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowGraphSupport() {}

    public record ResolvedGraph(
            String graphJson, List<WorkflowStepDTO> steps, List<List<String>> executionLayers) {}

    public static ResolvedGraph resolve(String graphJson, List<WorkflowStepDTO> fallbackSteps) {
        if (graphJson != null && !graphJson.isBlank()) {
            return fromGraphJson(graphJson);
        }
        if (fallbackSteps != null && !fallbackSteps.isEmpty()) {
            return fromLinearSteps(fallbackSteps);
        }
        throw new BusinessException("请在工作流画布中至少连接一个 Agent 节点");
    }

    public static String extractGraphJson(String dslJson, List<WorkflowStepDTO> steps) {
        if (dslJson != null && !dslJson.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(dslJson);
                JsonNode graph = root.get("graph");
                if (graph != null && !graph.isNull()) {
                    return MAPPER.writeValueAsString(graph);
                }
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return fromLinearSteps(steps).graphJson();
    }

    private static ResolvedGraph fromGraphJson(String graphJson) {
        try {
            JsonNode root = MAPPER.readTree(graphJson);
            JsonNode nodes = root.get("nodes");
            JsonNode edges = root.get("edges");
            if (nodes == null || !nodes.isArray() || edges == null || !edges.isArray()) {
                throw new BusinessException("工作流图格式无效");
            }

            Map<String, JsonNode> nodeById = new HashMap<>();
            String startId = null;
            String endId = null;
            List<JsonNode> agentNodes = new ArrayList<>();

            for (JsonNode n : nodes) {
                String id = text(n, "id");
                if (id == null) {
                    continue;
                }
                nodeById.put(id, n);
                String type = text(n, "type");
                if ("start".equals(type)) {
                    startId = id;
                } else if ("end".equals(type)) {
                    endId = id;
                } else if ("agent".equals(type)) {
                    agentNodes.add(n);
                }
            }
            if (startId == null || endId == null) {
                throw new BusinessException("画布需包含「开始」与「结束」节点");
            }
            if (agentNodes.isEmpty()) {
                throw new BusinessException("请至少添加一个 Agent 节点并连线");
            }

            Map<String, List<String>> adj = buildAdjacency(edges);
            Set<String> agentIdSet =
                    agentNodes.stream().map(n -> text(n, "id")).filter(Objects::nonNull).collect(Collectors.toSet());
            validateGraphReachable(startId, endId, agentIdSet, adj);
            List<String> orderedAgentIds = topoSortAgents(startId, endId, agentIdSet, adj);
            List<List<String>> executionLayers = buildExecutionLayers(startId, endId, agentIdSet, adj, orderedAgentIds);

            List<WorkflowStepDTO> steps = new ArrayList<>();
            int order = 1;
            for (String agentNodeId : orderedAgentIds) {
                JsonNode n = nodeById.get(agentNodeId);
                JsonNode data = n.get("data");
                Long agentId = data != null && data.has("agentId") ? data.get("agentId").asLong() : null;
                if (agentId == null) {
                    throw new BusinessException("Agent 节点未选择模型");
                }
                WorkflowStepDTO step = new WorkflowStepDTO();
                step.setAgentId(agentId);
                step.setSortOrder(order++);
                String nodeKey = data != null ? text(data, "nodeKey") : null;
                if (nodeKey == null || nodeKey.isBlank()) {
                    nodeKey = agentNodeId;
                }
                step.setNodeKey(nodeKey);
                step.setNodeLabel(data != null ? text(data, "label") : null);
                steps.add(step);
            }
            validateStepKeys(steps);
            return new ResolvedGraph(graphJson, steps, executionLayers);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析工作流图失败: " + e.getMessage());
        }
    }

    private static void validateGraphReachable(
            String startId, String endId, Set<String> agentIds, Map<String, List<String>> adj) {
        Set<String> fromStart = bfsReachable(startId, adj);
        for (String agentId : agentIds) {
            if (!fromStart.contains(agentId)) {
                throw new BusinessException("存在未从「开始」连通的 Agent 节点");
            }
            if (!hasPath(agentId, endId, adj)) {
                throw new BusinessException("存在未连接到「结束」的 Agent 节点");
            }
        }
    }

    private static Set<String> bfsReachable(String from, Map<String, List<String>> adj) {
        Set<String> seen = new HashSet<>();
        Queue<String> q = new ArrayDeque<>();
        q.add(from);
        while (!q.isEmpty()) {
            String c = q.poll();
            if (!seen.add(c)) {
                continue;
            }
            q.addAll(adj.getOrDefault(c, List.of()));
        }
        return seen;
    }

    /** 按画布 DAG 拓扑排序 Agent（支持分支/汇合）。 */
    private static List<String> topoSortAgents(
            String startId, String endId, Set<String> agentIds, Map<String, List<String>> adj) {
        Set<String> all = new HashSet<>();
        all.add(startId);
        all.add(endId);
        all.addAll(agentIds);

        Map<String, Integer> indeg = new HashMap<>();
        for (String n : all) {
            indeg.put(n, 0);
        }
        for (String n : all) {
            for (String succ : adj.getOrDefault(n, List.of())) {
                if (all.contains(succ)) {
                    indeg.merge(succ, 1, Integer::sum);
                }
            }
        }

        Queue<String> q = new ArrayDeque<>();
        for (String n : all) {
            if (indeg.get(n) == 0) {
                q.add(n);
            }
        }

        List<String> topo = new ArrayList<>();
        while (!q.isEmpty()) {
            String u = q.poll();
            topo.add(u);
            for (String succ : adj.getOrDefault(u, List.of())) {
                if (!all.contains(succ)) {
                    continue;
                }
                int next = indeg.merge(succ, -1, Integer::sum);
                if (next == 0) {
                    q.add(succ);
                }
            }
        }
        if (topo.size() != all.size()) {
            throw new BusinessException("工作流图存在环路，请检查连线");
        }
        return topo.stream().filter(agentIds::contains).toList();
    }

    /** 按依赖层级划分执行批次：同层节点可并行，层间按序推进。 */
    private static List<List<String>> buildExecutionLayers(
            String startId,
            String endId,
            Set<String> agentIds,
            Map<String, List<String>> adj,
            List<String> topoOrder) {
        Set<String> all = new HashSet<>();
        all.add(startId);
        all.add(endId);
        all.addAll(agentIds);

        Map<String, Integer> level = new HashMap<>();
        level.put(startId, 0);
        for (String node : topoOrder) {
            if (!all.contains(node)) {
                continue;
            }
            int base = level.getOrDefault(node, 0);
            for (String succ : adj.getOrDefault(node, List.of())) {
                if (!all.contains(succ)) {
                    continue;
                }
                level.put(succ, Math.max(level.getOrDefault(succ, 0), base + 1));
            }
        }

        int maxLevel = agentIds.stream().mapToInt(id -> level.getOrDefault(id, 0)).max().orElse(0);
        List<List<String>> layers = new ArrayList<>();
        for (int lv = 1; lv <= maxLevel; lv++) {
            int finalLv = lv;
            List<String> layer =
                    agentIds.stream()
                            .filter(id -> level.getOrDefault(id, 0) == finalLv)
                            .sorted()
                            .toList();
            if (!layer.isEmpty()) {
                layers.add(layer);
            }
        }
        if (layers.isEmpty()) {
            layers.add(List.copyOf(topoOrder.stream().filter(agentIds::contains).toList()));
        }
        return layers;
    }

    private static boolean hasPath(String from, String to, Map<String, List<String>> adj) {
        Set<String> seen = new HashSet<>();
        Queue<String> q = new ArrayDeque<>();
        q.add(from);
        while (!q.isEmpty()) {
            String c = q.poll();
            if (!seen.add(c)) {
                continue;
            }
            if (c.equals(to)) {
                return true;
            }
            q.addAll(adj.getOrDefault(c, List.of()));
        }
        return false;
    }

    private static Map<String, List<String>> buildAdjacency(JsonNode edges) {
        Map<String, List<String>> adj = new HashMap<>();
        for (JsonNode e : edges) {
            String source = text(e, "source");
            String target = text(e, "target");
            if (source == null || target == null) {
                continue;
            }
            adj.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
        }
        return adj;
    }

    private static ResolvedGraph fromLinearSteps(List<WorkflowStepDTO> steps) {
        List<WorkflowStepDTO> ordered =
                steps.stream()
                        .sorted(Comparator.comparing(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                        .toList();
        try {
            ObjectNode graph = MAPPER.createObjectNode();
            ArrayNode nodes = graph.putArray("nodes");
            ArrayNode edges = graph.putArray("edges");

            ObjectNode start = nodes.addObject();
            start.put("id", "start");
            start.put("type", "start");
            ObjectNode startPos = start.putObject("position");
            startPos.put("x", 80);
            startPos.put("y", 200);

            String prev = "start";
            int x = 280;
            int order = 1;
            for (WorkflowStepDTO step : ordered) {
                String id = step.getNodeKey() != null ? step.getNodeKey() : "node_" + order;
                ObjectNode agent = nodes.addObject();
                agent.put("id", id);
                agent.put("type", "agent");
                ObjectNode pos = agent.putObject("position");
                pos.put("x", x);
                pos.put("y", 200);
                ObjectNode data = agent.putObject("data");
                data.put("agentId", step.getAgentId());
                data.put("nodeKey", id);
                if (step.getNodeLabel() != null) {
                    data.put("label", step.getNodeLabel());
                }
                ObjectNode edge = edges.addObject();
                edge.put("id", "e-" + prev + "-" + id);
                edge.put("source", prev);
                edge.put("target", id);
                prev = id;
                x += 220;
                order++;
            }
            ObjectNode end = nodes.addObject();
            end.put("id", "end");
            end.put("type", "end");
            ObjectNode endPos = end.putObject("position");
            endPos.put("x", x);
            endPos.put("y", 200);
            ObjectNode endEdge = edges.addObject();
            endEdge.put("id", "e-" + prev + "-end");
            endEdge.put("source", prev);
            endEdge.put("target", "end");

            validateStepKeys(ordered);
            List<List<String>> layers = new ArrayList<>();
            for (WorkflowStepDTO step : ordered) {
                String key = step.getNodeKey() != null ? step.getNodeKey() : "node";
                layers.add(List.of(key));
            }
            return new ResolvedGraph(MAPPER.writeValueAsString(graph), ordered, layers);
        } catch (Exception e) {
            throw new BusinessException("生成默认工作流图失败");
        }
    }

    private static void validateStepKeys(List<WorkflowStepDTO> steps) {
        Set<String> keys = steps.stream().map(WorkflowStepDTO::getNodeKey).collect(Collectors.toSet());
        if (keys.size() != steps.size()) {
            throw new BusinessException("节点标识 nodeKey 不能重复");
        }
        Set<Long> agents = steps.stream().map(WorkflowStepDTO::getAgentId).collect(Collectors.toSet());
        if (agents.size() != steps.size()) {
            throw new BusinessException("同一工作流中 Agent 不能重复");
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
