package com.aios.platform.chat.service;

import com.aios.platform.ai.entity.AiWorkflow;
import com.aios.platform.ai.mapper.AiWorkflowMapper;
import com.aios.platform.chat.dto.ChatSessionVO;
import com.aios.platform.chat.entity.ChatMessage;
import com.aios.platform.chat.entity.ChatSession;
import com.aios.platform.chat.mapper.ChatMessageMapper;
import com.aios.platform.chat.mapper.ChatSessionMapper;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.ai.entity.AiTokenUsage;
import com.aios.platform.ai.mapper.AiTokenUsageMapper;
import com.aios.platform.ai.service.AiTokenUsageService;
import com.aios.platform.chat.dto.ChatTokenStatsVO;
import com.aios.platform.runtime.TokenEstimator;
import com.aios.platform.runtime.TokenUsageContext;
import com.aios.platform.runtime.WorkflowRuntimeService;
import com.aios.platform.runtime.dto.ChatExecutionResult;
import com.aios.platform.security.UserPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AiWorkflowMapper workflowMapper;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final AiTokenUsageService tokenUsageService;
    private final AiTokenUsageMapper tokenUsageMapper;

    public Page<ChatSessionVO> sessions(long current, long size) {
        Long uid = requireUserId();
        Page<ChatSession> raw =
                sessionMapper.selectPage(
                        new Page<>(current, size),
                        new LambdaQueryWrapper<ChatSession>()
                                .eq(ChatSession::getUserId, uid)
                                .orderByDesc(ChatSession::getUpdatedTime));
        Map<Long, String> workflowNames = loadWorkflowNames(raw.getRecords());
        Page<ChatSessionVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(s -> toVo(s, workflowNames)).toList());
        return out;
    }

    public List<ChatMessage> messages(Long sessionId) {
        ChatSession s = sessionMapper.selectById(sessionId);
        assertSessionOwner(s);
        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedTime));
    }

    @Transactional
    public Long createSession(Long agentId, Long workflowId, String title) {
        Long uid = requireUserId();
        validateWorkflow(workflowId);
        ChatSession s = new ChatSession();
        s.setUserId(uid);
        s.setAgentId(agentId);
        s.setWorkflowId(workflowId);
        s.setSessionTitle(title != null && !title.isBlank() ? title : "新会话");
        s.setSessionStatus("ACTIVE");
        sessionMapper.insert(s);
        return s.getSessionId();
    }

    @Transactional
    public void updateSession(Long sessionId, Long agentId, Long workflowId, String title) {
        ChatSession s = sessionMapper.selectById(sessionId);
        assertSessionOwner(s);
        if (workflowId != null) {
            validateWorkflow(workflowId);
            s.setWorkflowId(workflowId);
        }
        if (agentId != null) {
            s.setAgentId(agentId);
        }
        if (title != null && !title.isBlank()) {
            s.setSessionTitle(title);
        }
        sessionMapper.updateById(s);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession s = sessionMapper.selectById(sessionId);
        assertSessionOwner(s);
        messageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    public List<ChatMessage> sendUserMessage(Long sessionId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        ChatSession s = sessionMapper.selectById(sessionId);
        assertSessionOwner(s);
        if (s.getWorkflowId() == null) {
            throw new BusinessException("请先选择工作流再发送消息");
        }

        List<ChatMessage> historyBefore =
                messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getSessionId, sessionId)
                                .orderByAsc(ChatMessage::getCreatedTime));

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRoleType("user");
        userMsg.setContent(content);
        userMsg.setTokenCount(TokenEstimator.estimateText(content));
        userMsg.setResponseTime(0L);
        messageMapper.insert(userMsg);

        long t0 = System.currentTimeMillis();
        ChatExecutionResult execution =
                workflowRuntimeService.execute(s, content, historyBefore);

        int assistantTokens = execution.getTotalTokens() > 0 ? execution.getTotalTokens() : 0;

        ChatMessage bot = new ChatMessage();
        bot.setSessionId(sessionId);
        bot.setRoleType("assistant");
        bot.setContent(execution.getReply() != null ? execution.getReply() : "");
        bot.setTokenCount(assistantTokens);
        bot.setResponseTime(System.currentTimeMillis() - t0);
        messageMapper.insert(bot);

        TokenUsageContext usageCtx =
                TokenUsageContext.builder()
                        .userId(s.getUserId())
                        .sessionId(sessionId)
                        .workflowId(s.getWorkflowId())
                        .messageId(bot.getMessageId())
                        .build();
        tokenUsageService.recordBatch(usageCtx, execution.getUsageRecords());

        s.setSessionTitle(truncateTitle(content));
        sessionMapper.updateById(s);

        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedTime));
    }

    public ChatTokenStatsVO tokenStats(Long sessionId) {
        ChatSession s = sessionMapper.selectById(sessionId);
        assertSessionOwner(s);

        List<ChatMessage> messages =
                messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        int messageTotal =
                messages.stream()
                        .map(ChatMessage::getTokenCount)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();

        List<AiTokenUsage> usages =
                tokenUsageMapper.selectList(
                        new LambdaQueryWrapper<AiTokenUsage>().eq(AiTokenUsage::getSessionId, sessionId));
        int prompt = 0;
        int completion = 0;
        int total = 0;
        for (AiTokenUsage u : usages) {
            prompt += u.getPromptTokens() != null ? u.getPromptTokens() : 0;
            completion += u.getCompletionTokens() != null ? u.getCompletionTokens() : 0;
            total += u.getTotalTokens() != null ? u.getTotalTokens() : 0;
        }

        ChatTokenStatsVO vo = new ChatTokenStatsVO();
        vo.setSessionId(sessionId);
        vo.setMessageTotalTokens(messageTotal);
        vo.setUsageRecordPromptTokens(prompt);
        vo.setUsageRecordCompletionTokens(completion);
        vo.setUsageRecordTotalTokens(total);
        vo.setLlmCallCount(usages.size());
        return vo;
    }

    private void validateWorkflow(Long workflowId) {
        if (workflowId == null) {
            return;
        }
        AiWorkflow w = workflowMapper.selectById(workflowId);
        if (w == null) {
            throw new BusinessException("工作流不存在");
        }
        if (w.getStatus() != null && w.getStatus() == 0) {
            throw new BusinessException("工作流已停用");
        }
    }

    private Map<Long, String> loadWorkflowNames(List<ChatSession> sessions) {
        Set<Long> ids =
                sessions.stream()
                        .map(ChatSession::getWorkflowId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return workflowMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AiWorkflow::getWorkflowId, AiWorkflow::getWorkflowName, (a, b) -> a));
    }

    private ChatSessionVO toVo(ChatSession s, Map<Long, String> workflowNames) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setSessionId(s.getSessionId());
        vo.setUserId(s.getUserId());
        vo.setAgentId(s.getAgentId());
        vo.setWorkflowId(s.getWorkflowId());
        if (s.getWorkflowId() != null) {
            vo.setWorkflowName(workflowNames.get(s.getWorkflowId()));
        }
        vo.setSessionTitle(s.getSessionTitle());
        vo.setSessionStatus(s.getSessionStatus());
        vo.setCreatedTime(s.getCreatedTime());
        vo.setUpdatedTime(s.getUpdatedTime());
        return vo;
    }

    private static String truncateTitle(String content) {
        String t = content.replace("\n", " ").trim();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }

    private void assertSessionOwner(ChatSession s) {
        if (s == null) {
            throw new BusinessException("会话不存在");
        }
        Long uid = requireUserId();
        if (s.getUserId() == null || !s.getUserId().equals(uid)) {
            throw new BusinessException(403, "无权访问该会话");
        }
    }

    private Long requireUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getUserId();
        }
        throw new BusinessException(401, "未登录");
    }
}
