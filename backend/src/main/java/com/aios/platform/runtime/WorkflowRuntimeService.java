package com.aios.platform.runtime;

import com.aios.platform.ai.entity.AiWorkflow;
import com.aios.platform.ai.mapper.AiWorkflowMapper;
import com.aios.platform.chat.entity.ChatMessage;
import com.aios.platform.chat.entity.ChatSession;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.runtime.dto.AgentStepResult;
import com.aios.platform.runtime.dto.ChatExecutionResult;
import com.aios.platform.runtime.dto.PendingTokenUsageRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowRuntimeService {

    private final AiWorkflowMapper workflowMapper;
    private final AgentRuntimeService agentRuntimeService;

    public ChatExecutionResult execute(ChatSession session, String userMessage, List<ChatMessage> history) {
        if (session.getWorkflowId() == null) {
            throw new BusinessException("请先为会话绑定工作流后再发送消息");
        }
        AiWorkflow workflow = workflowMapper.selectById(session.getWorkflowId());
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        return executeWorkflow(workflow, userMessage, history, true);
    }

    public ChatExecutionResult executeWorkflow(
            AiWorkflow workflow, String userMessage, List<ChatMessage> history, boolean appendTrace) {
        if (workflow.getStatus() != null && workflow.getStatus() == 0) {
            throw new BusinessException("工作流已停用");
        }
        if (workflow.getDslJson() == null || workflow.getDslJson().isBlank()) {
            throw new BusinessException("工作流 DSL 未配置，请在工作流管理中保存画布");
        }

        WorkflowExecutionPlan plan = WorkflowDslParser.parse(workflow.getWorkflowName(), workflow.getDslJson());
        Map<String, String> stepOutputs = new LinkedHashMap<>();
        List<AgentStepResult> allSteps = new ArrayList<>();
        String lastReply = "";

        for (List<String> layerKeys : plan.layers()) {
            String upstream = formatUpstream(stepOutputs);
            List<CompletableFuture<AgentStepResult>> futures = new ArrayList<>();
            for (String nodeKey : layerKeys) {
                WorkflowExecutionPlan.WorkflowNodeSpec node = plan.nodesByKey().get(nodeKey);
                if (node == null) {
                    continue;
                }
                futures.add(
                        CompletableFuture.supplyAsync(
                                () -> agentRuntimeService.runStep(node, userMessage, upstream, history)));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            for (CompletableFuture<AgentStepResult> f : futures) {
                AgentStepResult step = f.join();
                allSteps.add(step);
                if (step.getOutput() != null) {
                    stepOutputs.put(step.getNodeKey(), step.getOutput());
                    lastReply = step.getOutput();
                }
            }
        }

        if (lastReply.isBlank() && !allSteps.isEmpty()) {
            lastReply = allSteps.get(allSteps.size() - 1).getOutput();
        }
        if (lastReply == null) {
            lastReply = "";
        }

        int promptSum = 0;
        int completionSum = 0;
        int totalSum = 0;
        List<PendingTokenUsageRecord> usageRecords = new ArrayList<>();
        for (AgentStepResult step : allSteps) {
            promptSum += step.getPromptTokens();
            completionSum += step.getCompletionTokens();
            totalSum += step.getTotalTokens();
            if (step.getTotalTokens() > 0) {
                PendingTokenUsageRecord rec = new PendingTokenUsageRecord();
                rec.setAgentId(step.getAgentId());
                rec.setModelCode(step.getModelCode());
                rec.setUsage(
                        new LlmTokenUsage(
                                step.getPromptTokens(), step.getCompletionTokens(), step.getTotalTokens()));
                usageRecords.add(rec);
            }
        }

        ChatExecutionResult result = new ChatExecutionResult();
        result.setWorkflowId(workflow.getWorkflowId());
        result.setWorkflowName(workflow.getWorkflowName());
        result.setSteps(allSteps);
        result.setPromptTokens(promptSum);
        result.setCompletionTokens(completionSum);
        result.setTotalTokens(totalSum);
        result.setUsageRecords(usageRecords);
        result.setReply(
                appendTrace ? formatReply(lastReply, plan, allSteps, totalSum) : lastReply);
        return result;
    }

    private static String formatUpstream(Map<String, String> stepOutputs) {
        if (stepOutputs.isEmpty()) {
            return "";
        }
        return stepOutputs.entrySet().stream()
                .map(e -> "- [" + e.getKey() + "]\n" + e.getValue())
                .collect(Collectors.joining("\n\n"));
    }

    private static String formatReply(
            String lastReply, WorkflowExecutionPlan plan, List<AgentStepResult> steps, int totalTokens) {
        if (plan.layers().size() <= 1 && steps.size() <= 1) {
            return lastReply;
        }
        StringBuilder sb = new StringBuilder(lastReply);
        sb.append("\n\n---\n**工作流执行**（").append(plan.workflowName()).append("）\n");
        for (AgentStepResult s : steps) {
            sb.append("- `")
                    .append(s.getNodeKey())
                    .append("` ")
                    .append(s.getAgentName() != null ? s.getAgentName() : "")
                    .append(" · ")
                    .append(s.getElapsedMs())
                    .append("ms");
            if (s.getTotalTokens() > 0) {
                sb.append(" · ").append(s.getTotalTokens()).append(" tokens");
            }
            if (s.getError() != null) {
                sb.append(" · 异常: ").append(s.getError());
            }
            sb.append("\n");
        }
        if (totalTokens > 0) {
            sb.append("\n**合计 Token**：").append(totalTokens);
        }
        return sb.toString();
    }
}
