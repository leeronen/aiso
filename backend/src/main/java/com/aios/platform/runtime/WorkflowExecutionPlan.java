package com.aios.platform.runtime;

import java.util.List;
import java.util.Map;

/** 从工作流 DSL 解析出的执行计划。 */
public record WorkflowExecutionPlan(
        String workflowName,
        List<List<String>> layers,
        Map<String, WorkflowNodeSpec> nodesByKey) {

    public record WorkflowNodeSpec(String nodeKey, long agentId, String label, int order) {}
}
