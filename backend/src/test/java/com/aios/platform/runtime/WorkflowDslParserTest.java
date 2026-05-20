package com.aios.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aios.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class WorkflowDslParserTest {

    @Test
    void parse_usesExecutionLayers() {
        String dsl =
                """
                {
                  "nodes": [
                    {"key": "node_1", "agentId": 1, "order": 1},
                    {"key": "node_2", "agentId": 2, "order": 2}
                  ],
                  "executionLayers": [["node_1"], ["node_2"]]
                }
                """;
        WorkflowExecutionPlan plan = WorkflowDslParser.parse("wf-a", dsl);
        assertEquals("wf-a", plan.workflowName());
        assertEquals(2, plan.layers().size());
        assertEquals("node_1", plan.layers().get(0).get(0));
        assertEquals(2L, plan.nodesByKey().get("node_2").agentId());
    }

    @Test
    void parse_fallsBackToOrderWhenNoLayers() {
        String dsl =
                """
                {
                  "nodes": [
                    {"key": "b", "agentId": 10, "order": 2},
                    {"key": "a", "agentId": 9, "order": 1}
                  ]
                }
                """;
        WorkflowExecutionPlan plan = WorkflowDslParser.parse("wf-b", dsl);
        assertEquals(2, plan.layers().size());
        assertEquals("a", plan.layers().get(0).get(0));
        assertEquals("b", plan.layers().get(1).get(0));
    }

    @Test
    void parse_rejectsMissingAgentId() {
        String dsl =
                """
                {"nodes": [{"key": "n1", "agentId": 0}]}
                """;
        BusinessException ex =
                assertThrows(BusinessException.class, () -> WorkflowDslParser.parse("x", dsl));
        assertTrue(ex.getMessage().contains("未绑定 Agent"));
    }

    @Test
    void parse_rejectsEmptyDsl() {
        assertThrows(BusinessException.class, () -> WorkflowDslParser.parse("x", "  "));
    }
}
