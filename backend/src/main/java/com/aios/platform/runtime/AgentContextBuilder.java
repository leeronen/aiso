package com.aios.platform.runtime;

import com.aios.platform.ai.entity.AiAgent;
import com.aios.platform.ai.entity.AiMcpServer;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.kb.dto.KbRagHitVO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgentContextBuilder {

    public String buildSystemPrompt(
            AiAgent agent,
            String toolMode,
            List<AiMcpServer> mcpServers,
            List<AiSkill> skills,
            List<KbRagHitVO> ragHits) {
        StringBuilder sb = new StringBuilder();
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()) {
            sb.append(agent.getSystemPrompt().trim());
        } else {
            sb.append("你是 AIOS 平台中的智能助手「").append(agent.getAgentName()).append("」。");
        }

        appendThinkingMode(sb, agent.getThinkingMode());
        appendKnowledge(sb, ragHits, agent);
        appendTools(sb, toolMode, mcpServers, skills);
        return sb.toString();
    }

    public String buildStepUserContent(
            String userMessage, String upstreamSummary, String nodeLabel, String agentName) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户问题】\n").append(userMessage.trim()).append("\n\n");
        if (upstreamSummary != null && !upstreamSummary.isBlank()) {
            sb.append("【工作流上游节点输出】\n").append(upstreamSummary.trim()).append("\n\n");
        }
        String role = nodeLabel != null && !nodeLabel.isBlank() ? nodeLabel : agentName;
        sb.append("【当前步骤】请以「").append(role).append("」的角色完成本节点任务，输出清晰、可给下游节点使用的结论。");
        return sb.toString();
    }

    private void appendThinkingMode(StringBuilder sb, String thinkingMode) {
        if (thinkingMode == null || thinkingMode.isBlank()) {
            return;
        }
        sb.append("\n\n【思考模式】").append(thinkingMode);
        if ("ReAct".equalsIgnoreCase(thinkingMode)) {
            sb.append(
                    "：请按 Thought → Action → Observation 循环推理，在最终回答中给出明确结论，无需暴露完整中间链。");
        } else if ("CoT".equalsIgnoreCase(thinkingMode) || "chain".equalsIgnoreCase(thinkingMode)) {
            sb.append("：请逐步推理后再给出最终答案。");
        }
    }

    private void appendKnowledge(StringBuilder sb, List<KbRagHitVO> hits, AiAgent agent) {
        boolean kbOn = agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1;
        if (!kbOn || hits == null || hits.isEmpty()) {
            return;
        }
        sb.append("\n\n【知识库检索片段】（请优先依据以下内容回答，不足处可结合常识补充）\n");
        int i = 1;
        for (KbRagHitVO h : hits) {
            if (h.getContent() == null || h.getContent().isBlank()) {
                continue;
            }
            sb.append(i++).append(". ");
            if (h.getScore() != null) {
                sb.append("(相关度 ").append(String.format("%.2f", h.getScore())).append(") ");
            }
            sb.append(h.getContent().trim()).append("\n");
        }
    }

    private void appendTools(
            StringBuilder sb, String toolMode, List<AiMcpServer> mcpServers, List<AiSkill> skills) {
        String mode = toolMode != null ? toolMode : "auto";
        if ("none".equals(mode)) {
            return;
        }
        boolean includeMcp = "auto".equals(mode) || "manual".equals(mode) || "mcp_only".equals(mode);
        boolean includeSkill = "auto".equals(mode) || "manual".equals(mode) || "skill_only".equals(mode);

        if (includeMcp && mcpServers != null && !mcpServers.isEmpty()) {
            sb.append("\n\n【可用 MCP 工具服务】（根据用户问题判断是否需调用；当前为描述注入，实际协议调用由平台后续接入）\n");
            for (AiMcpServer m : mcpServers) {
                sb.append("- ")
                        .append(m.getServerName())
                        .append(" [")
                        .append(m.getProtocolType() != null ? m.getProtocolType() : "http")
                        .append("] ");
                if (m.getServerUrl() != null) {
                    sb.append(m.getServerUrl());
                }
                sb.append("\n");
            }
        }
        if (includeSkill && skills != null && !skills.isEmpty()) {
            sb.append("\n\n【已绑定 Skill】\n");
            for (AiSkill sk : skills) {
                sb.append("- ").append(sk.getSkillName());
                if (sk.getDescription() != null && !sk.getDescription().isBlank()) {
                    sb.append("：").append(sk.getDescription().trim());
                }
                if (sk.getWorkflowId() != null) {
                    sb.append("（关联子工作流 #").append(sk.getWorkflowId()).append("）");
                }
                if (sk.getMcpServerId() != null) {
                    sb.append("（关联 MCP #").append(sk.getMcpServerId()).append("）");
                }
                sb.append("\n");
            }
        }
        if (includeMcp || includeSkill) {
            sb.append("\n若问题需要外部工具，请在回答中说明拟使用的工具及步骤；若无需工具则直接作答。");
        }
    }
}
