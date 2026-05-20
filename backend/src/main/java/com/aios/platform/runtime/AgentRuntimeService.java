package com.aios.platform.runtime;

import com.aios.platform.ai.entity.AiAgent;
import com.aios.platform.ai.entity.AiAgentKnowledgeBase;
import com.aios.platform.ai.entity.AiAgentMcpServer;
import com.aios.platform.ai.entity.AiAgentSkill;
import com.aios.platform.ai.entity.AiMcpServer;
import com.aios.platform.ai.entity.AiModel;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.ai.mapper.AiAgentKnowledgeBaseMapper;
import com.aios.platform.ai.mapper.AiAgentMapper;
import com.aios.platform.ai.mapper.AiAgentMcpServerMapper;
import com.aios.platform.ai.mapper.AiAgentSkillMapper;
import com.aios.platform.ai.mapper.AiMcpServerMapper;
import com.aios.platform.ai.mapper.AiModelMapper;
import com.aios.platform.ai.mapper.AiSkillMapper;
import com.aios.platform.chat.entity.ChatMessage;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.dto.KbRagHitVO;
import com.aios.platform.kb.service.KbVectorStoreService;
import com.aios.platform.runtime.dto.AgentStepResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRuntimeService {

    private static final int HISTORY_LIMIT = 24;
    private static final int RAG_TOP_K = 4;

    private final AiAgentMapper agentMapper;
    private final AiModelMapper modelMapper;
    private final AiAgentKnowledgeBaseMapper agentKbMapper;
    private final AiAgentMcpServerMapper agentMcpMapper;
    private final AiAgentSkillMapper agentSkillMapper;
    private final AiMcpServerMapper mcpServerMapper;
    private final AiSkillMapper skillMapper;
    private final AgentContextBuilder contextBuilder;
    private final LlmChatClient llmChatClient;
    private final ObjectProvider<KbVectorStoreService> vectorStoreProvider;

    public AgentStepResult runStep(
            WorkflowExecutionPlan.WorkflowNodeSpec node,
            String userMessage,
            String upstreamSummary,
            List<ChatMessage> history) {
        long t0 = System.currentTimeMillis();
        AgentStepResult step = new AgentStepResult();
        step.setNodeKey(node.nodeKey());
        step.setNodeLabel(node.label());
        step.setAgentId(node.agentId());

        try {
            AiAgent agent = agentMapper.selectById(node.agentId());
            if (agent == null) {
                throw new BusinessException("Agent 不存在: " + node.agentId());
            }
            if (agent.getStatus() != null && agent.getStatus() == 0) {
                throw new BusinessException("Agent「" + agent.getAgentName() + "」已停用");
            }
            step.setAgentName(agent.getAgentName());

            AiModel model = modelMapper.selectById(agent.getModelId());
            if (model == null) {
                throw new BusinessException("Agent「" + agent.getAgentName() + "」未绑定有效模型");
            }

            List<Long> kbIds = loadKbIds(node.agentId());
            List<AiMcpServer> mcpServers = loadMcpServers(node.agentId(), agent.getToolMode());
            List<AiSkill> skills = loadSkills(node.agentId(), agent.getToolMode());
            List<KbRagHitVO> ragHits = retrieveKnowledge(agent, kbIds, userMessage);

            String system = contextBuilder.buildSystemPrompt(agent, agent.getToolMode(), mcpServers, skills, ragHits);
            String userContent =
                    contextBuilder.buildStepUserContent(
                            userMessage, upstreamSummary, node.label(), agent.getAgentName());

            List<LlmMessage> messages = new ArrayList<>();
            messages.add(new LlmMessage("system", system));
            appendHistory(messages, agent.getMemoryMode(), history);
            messages.add(new LlmMessage("user", userContent));

            step.setModelCode(model.getModelCode());
            LlmChatResult chatResult = llmChatClient.chat(model, messages);
            step.setOutput(chatResult.content());
            LlmTokenUsage usage = chatResult.usage();
            step.setPromptTokens(usage.promptTokens());
            step.setCompletionTokens(usage.completionTokens());
            step.setTotalTokens(usage.totalTokens());
        } catch (Exception e) {
            log.warn("Agent step failed node={} agentId={}: {}", node.nodeKey(), node.agentId(), e.getMessage());
            step.setError(e.getMessage());
            step.setOutput("【节点执行失败】" + e.getMessage());
        }
        step.setElapsedMs(System.currentTimeMillis() - t0);
        return step;
    }

    private List<KbRagHitVO> retrieveKnowledge(AiAgent agent, List<Long> kbIds, String query) {
        boolean kbOn = agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1;
        if (!kbOn || kbIds.isEmpty()) {
            return List.of();
        }
        KbVectorStoreService vector = vectorStoreProvider.getIfAvailable();
        if (vector == null) {
            log.debug("Vector store unavailable, skip RAG");
            return List.of();
        }
        List<KbRagHitVO> all = new ArrayList<>();
        for (Long kbId : kbIds) {
            try {
                all.addAll(vector.search(kbId, query, RAG_TOP_K));
            } catch (Exception e) {
                log.warn("RAG search failed kbId={}: {}", kbId, e.getMessage());
            }
        }
        return all.stream()
                .filter(h -> h.getContent() != null && !h.getContent().isBlank())
                .sorted(Comparator.comparing(KbRagHitVO::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RAG_TOP_K * 2L)
                .toList();
    }

    private void appendHistory(List<LlmMessage> messages, String memoryMode, List<ChatMessage> history) {
        String mode = memoryMode != null ? memoryMode : "session";
        if ("none".equals(mode) || history == null || history.isEmpty()) {
            return;
        }
        List<ChatMessage> slice =
                history.size() > HISTORY_LIMIT
                        ? history.subList(history.size() - HISTORY_LIMIT, history.size())
                        : history;
        for (ChatMessage m : slice) {
            if (m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            String role = mapRole(m.getRoleType());
            if (role == null) {
                continue;
            }
            messages.add(new LlmMessage(role, m.getContent()));
        }
    }

    private static String mapRole(String roleType) {
        if (roleType == null) {
            return null;
        }
        return switch (roleType.toLowerCase()) {
            case "user" -> "user";
            case "assistant", "bot" -> "assistant";
            case "system" -> "system";
            default -> null;
        };
    }

    private List<Long> loadKbIds(long agentId) {
        return agentKbMapper
                .selectList(
                        new LambdaQueryWrapper<AiAgentKnowledgeBase>().eq(AiAgentKnowledgeBase::getAgentId, agentId))
                .stream()
                .map(AiAgentKnowledgeBase::getKnowledgeBaseId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<AiMcpServer> loadMcpServers(long agentId, String toolMode) {
        if ("none".equals(toolMode) || "skill_only".equals(toolMode)) {
            return List.of();
        }
        List<Long> ids =
                agentMcpMapper
                        .selectList(
                                new LambdaQueryWrapper<AiAgentMcpServer>().eq(AiAgentMcpServer::getAgentId, agentId))
                        .stream()
                        .map(AiAgentMcpServer::getMcpServerId)
                        .filter(Objects::nonNull)
                        .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return mcpServerMapper.selectBatchIds(ids).stream()
                .filter(m -> m.getStatus() == null || m.getStatus() == 1)
                .toList();
    }

    private List<AiSkill> loadSkills(long agentId, String toolMode) {
        if ("none".equals(toolMode) || "mcp_only".equals(toolMode)) {
            return List.of();
        }
        List<Long> ids =
                agentSkillMapper
                        .selectList(new LambdaQueryWrapper<AiAgentSkill>().eq(AiAgentSkill::getAgentId, agentId))
                        .stream()
                        .map(AiAgentSkill::getSkillId)
                        .filter(Objects::nonNull)
                        .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return skillMapper.selectBatchIds(ids).stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .toList();
    }
}
