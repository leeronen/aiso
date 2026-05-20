package com.aios.platform.ai.service;

import com.aios.platform.ai.dto.AiAgentSaveRequest;
import com.aios.platform.ai.dto.AiAgentVO;
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
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.entity.KbKnowledgeBase;
import com.aios.platform.kb.mapper.KbKnowledgeBaseMapper;
import com.aios.platform.system.service.SysConfigOptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAgentService {

    private final AiAgentMapper agentMapper;
    private final AiModelMapper modelMapper;
    private final AiAgentKnowledgeBaseMapper agentKbMapper;
    private final AiAgentMcpServerMapper agentMcpMapper;
    private final AiAgentSkillMapper agentSkillMapper;
    private final KbKnowledgeBaseMapper kbMapper;
    private final AiMcpServerMapper mcpServerMapper;
    private final AiSkillMapper skillMapper;
    private final SysConfigOptionService configOptionService;

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AiAgent> q = new LambdaQueryWrapper<AiAgent>().eq(AiAgent::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiAgent::getAgentName, keyword).or().like(AiAgent::getDescription, keyword));
        }
        q.orderByAsc(AiAgent::getAgentName).last("LIMIT " + size);
        return agentMapper.selectList(q).stream()
                .map(a -> new SelectOptionVO(a.getAgentId(), a.getAgentName()))
                .toList();
    }

    public Page<AiAgentVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AiAgent> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiAgent::getAgentName, keyword).or().like(AiAgent::getDescription, keyword));
        }
        q.orderByDesc(AiAgent::getCreatedTime);
        Page<AiAgent> raw = agentMapper.selectPage(new Page<>(current, size), q);
        Map<Long, String> modelNames = loadModelNames(raw.getRecords());
        Map<Long, List<Long>> kbMap = loadKbIds(raw.getRecords());
        Map<Long, List<Long>> mcpMap = loadMcpIds(raw.getRecords());
        Map<Long, List<Long>> skillMap = loadSkillIds(raw.getRecords());
        Page<AiAgentVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(a -> toVo(a, modelNames, kbMap, mcpMap, skillMap, false))
                .toList());
        return out;
    }

    public AiAgentVO get(Long id) {
        AiAgent a = agentMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("Agent 不存在");
        }
        Map<Long, String> modelNames = loadModelNames(List.of(a));
        Map<Long, List<Long>> kbMap = loadKbIds(List.of(a));
        Map<Long, List<Long>> mcpMap = loadMcpIds(List.of(a));
        Map<Long, List<Long>> skillMap = loadSkillIds(List.of(a));
        return toVo(a, modelNames, kbMap, mcpMap, skillMap, true);
    }

    @Transactional
    public Long save(AiAgentSaveRequest req) {
        if (req.getAgentName() == null || req.getAgentName().isBlank()) {
            throw new BusinessException("Agent 名称不能为空");
        }
        if (req.getModelId() == null) {
            throw new BusinessException("请绑定模型");
        }
        if (modelMapper.selectById(req.getModelId()) == null) {
            throw new BusinessException("绑定的模型不存在");
        }
        String memoryMode = req.getMemoryMode() != null ? req.getMemoryMode() : "session";
        String toolMode = req.getToolMode() != null ? req.getToolMode() : "auto";
        configOptionService.validateCode(SysConfigOptionService.AGENT_MEMORY_MODE, memoryMode, "记忆模式");
        configOptionService.validateCode(SysConfigOptionService.AGENT_TOOL_MODE, toolMode, "工具模式");

        List<Long> kbIds = normalizeIds(req.getKnowledgeBaseIds());
        List<Long> mcpIds = normalizeIds(req.getMcpServerIds());
        List<Long> skillIds = normalizeIds(req.getSkillIds());

        AiAgent body = new AiAgent();
        body.setAgentId(req.getAgentId());
        body.setAgentName(req.getAgentName());
        body.setDescription(req.getDescription());
        body.setAvatar(req.getAvatar());
        body.setModelId(req.getModelId());
        body.setSystemPrompt(req.getSystemPrompt());
        body.setWelcomeMessage(req.getWelcomeMessage());
        body.setThinkingMode(req.getThinkingMode());
        body.setMemoryMode(memoryMode);
        body.setToolMode(toolMode);
        body.setMemoryEnabled("none".equals(memoryMode) ? 0 : 1);
        body.setKnowledgeEnabled(kbIds.isEmpty() ? 0 : 1);
        body.setToolEnabled("none".equals(toolMode) ? 0 : 1);
        body.setStatus(req.getStatus() != null ? req.getStatus() : 1);

        Long agentId;
        if (body.getAgentId() == null) {
            agentMapper.insert(body);
            agentId = body.getAgentId();
        } else {
            AiAgent db = agentMapper.selectById(body.getAgentId());
            if (db == null) {
                throw new BusinessException("Agent 不存在");
            }
            db.setAgentName(body.getAgentName());
            db.setDescription(body.getDescription());
            db.setAvatar(body.getAvatar());
            db.setModelId(body.getModelId());
            db.setSystemPrompt(body.getSystemPrompt());
            db.setWelcomeMessage(body.getWelcomeMessage());
            db.setThinkingMode(body.getThinkingMode());
            db.setMemoryMode(body.getMemoryMode());
            db.setToolMode(body.getToolMode());
            db.setMemoryEnabled(body.getMemoryEnabled());
            db.setKnowledgeEnabled(body.getKnowledgeEnabled());
            db.setToolEnabled(body.getToolEnabled());
            db.setStatus(body.getStatus());
            agentMapper.updateById(db);
            agentId = db.getAgentId();
        }

        replaceRelations(agentId, kbIds, mcpIds, skillIds);
        return agentId;
    }

    @Transactional
    public void delete(Long id) {
        agentKbMapper.delete(new LambdaQueryWrapper<AiAgentKnowledgeBase>().eq(AiAgentKnowledgeBase::getAgentId, id));
        agentMcpMapper.delete(new LambdaQueryWrapper<AiAgentMcpServer>().eq(AiAgentMcpServer::getAgentId, id));
        agentSkillMapper.delete(new LambdaQueryWrapper<AiAgentSkill>().eq(AiAgentSkill::getAgentId, id));
        agentMapper.deleteById(id);
    }

    private void replaceRelations(Long agentId, List<Long> kbIds, List<Long> mcpIds, List<Long> skillIds) {
        agentKbMapper.delete(new LambdaQueryWrapper<AiAgentKnowledgeBase>().eq(AiAgentKnowledgeBase::getAgentId, agentId));
        agentMcpMapper.delete(new LambdaQueryWrapper<AiAgentMcpServer>().eq(AiAgentMcpServer::getAgentId, agentId));
        agentSkillMapper.delete(new LambdaQueryWrapper<AiAgentSkill>().eq(AiAgentSkill::getAgentId, agentId));

        for (Long kbId : kbIds) {
            AiAgentKnowledgeBase rel = new AiAgentKnowledgeBase();
            rel.setAgentId(agentId);
            rel.setKnowledgeBaseId(kbId);
            agentKbMapper.insert(rel);
        }
        for (Long mcpId : mcpIds) {
            AiAgentMcpServer rel = new AiAgentMcpServer();
            rel.setAgentId(agentId);
            rel.setMcpServerId(mcpId);
            agentMcpMapper.insert(rel);
        }
        for (Long skillId : skillIds) {
            AiAgentSkill rel = new AiAgentSkill();
            rel.setAgentId(agentId);
            rel.setSkillId(skillId);
            agentSkillMapper.insert(rel);
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private Map<Long, String> loadModelNames(List<AiAgent> agents) {
        Set<Long> ids = agents.stream().map(AiAgent::getModelId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return modelMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AiModel::getModelId, AiModel::getModelName, (a, b) -> a));
    }

    private Map<Long, List<Long>> loadKbIds(List<AiAgent> agents) {
        if (agents.isEmpty()) {
            return Map.of();
        }
        List<AiAgentKnowledgeBase> rels = agentKbMapper.selectList(
                new LambdaQueryWrapper<AiAgentKnowledgeBase>()
                        .in(AiAgentKnowledgeBase::getAgentId, agents.stream().map(AiAgent::getAgentId).toList()));
        return rels.stream()
                .collect(Collectors.groupingBy(
                        AiAgentKnowledgeBase::getAgentId,
                        Collectors.mapping(AiAgentKnowledgeBase::getKnowledgeBaseId, Collectors.toList())));
    }

    private Map<Long, List<Long>> loadMcpIds(List<AiAgent> agents) {
        if (agents.isEmpty()) {
            return Map.of();
        }
        List<AiAgentMcpServer> rels = agentMcpMapper.selectList(
                new LambdaQueryWrapper<AiAgentMcpServer>()
                        .in(AiAgentMcpServer::getAgentId, agents.stream().map(AiAgent::getAgentId).toList()));
        return rels.stream()
                .collect(Collectors.groupingBy(
                        AiAgentMcpServer::getAgentId,
                        Collectors.mapping(AiAgentMcpServer::getMcpServerId, Collectors.toList())));
    }

    private Map<Long, List<Long>> loadSkillIds(List<AiAgent> agents) {
        if (agents.isEmpty()) {
            return Map.of();
        }
        List<AiAgentSkill> rels = agentSkillMapper.selectList(
                new LambdaQueryWrapper<AiAgentSkill>()
                        .in(AiAgentSkill::getAgentId, agents.stream().map(AiAgent::getAgentId).toList()));
        return rels.stream()
                .collect(Collectors.groupingBy(
                        AiAgentSkill::getAgentId, Collectors.mapping(AiAgentSkill::getSkillId, Collectors.toList())));
    }

    private AiAgentVO toVo(
            AiAgent a,
            Map<Long, String> modelNames,
            Map<Long, List<Long>> kbMap,
            Map<Long, List<Long>> mcpMap,
            Map<Long, List<Long>> skillMap,
            boolean withIds) {
        AiAgentVO vo = new AiAgentVO();
        vo.setAgentId(a.getAgentId());
        vo.setAgentName(a.getAgentName());
        vo.setDescription(a.getDescription());
        vo.setAvatar(a.getAvatar());
        vo.setModelId(a.getModelId());
        vo.setModelName(a.getModelId() != null ? modelNames.get(a.getModelId()) : null);
        vo.setSystemPrompt(a.getSystemPrompt());
        vo.setWelcomeMessage(a.getWelcomeMessage());
        vo.setThinkingMode(a.getThinkingMode());
        vo.setMemoryMode(a.getMemoryMode());
        vo.setToolMode(a.getToolMode());
        vo.setMemoryModeLabel(configOptionService.labelOf(SysConfigOptionService.AGENT_MEMORY_MODE, a.getMemoryMode()));
        vo.setToolModeLabel(configOptionService.labelOf(SysConfigOptionService.AGENT_TOOL_MODE, a.getToolMode()));
        vo.setMemoryEnabled(a.getMemoryEnabled());
        vo.setKnowledgeEnabled(a.getKnowledgeEnabled());
        vo.setToolEnabled(a.getToolEnabled());
        vo.setStatus(a.getStatus());
        vo.setCreatedTime(a.getCreatedTime());

        List<Long> kbIds = kbMap.getOrDefault(a.getAgentId(), List.of());
        List<Long> mcpIds = mcpMap.getOrDefault(a.getAgentId(), List.of());
        List<Long> skillIds = skillMap.getOrDefault(a.getAgentId(), List.of());

        if (withIds) {
            vo.setKnowledgeBaseIds(kbIds);
            vo.setMcpServerIds(mcpIds);
            vo.setSkillIds(skillIds);
        }
        vo.setKnowledgeBaseSummary(summarizeNames(kbIds, loadKbNames(kbIds)));
        vo.setMcpServerSummary(summarizeNames(mcpIds, loadMcpNames(mcpIds)));
        vo.setSkillSummary(summarizeNames(skillIds, loadSkillNames(skillIds)));
        return vo;
    }

    private Map<Long, String> loadKbNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return kbMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(KbKnowledgeBase::getKnowledgeBaseId, KbKnowledgeBase::getKnowledgeBaseName, (a, b) -> a));
    }

    private Map<Long, String> loadMcpNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mcpServerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AiMcpServer::getMcpServerId, AiMcpServer::getServerName, (a, b) -> a));
    }

    private Map<Long, String> loadSkillNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return skillMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AiSkill::getSkillId, AiSkill::getSkillName, (a, b) -> a));
    }

    private String summarizeNames(List<Long> ids, Map<Long, String> names) {
        if (ids.isEmpty()) {
            return "—";
        }
        String joined = ids.stream()
                .map(id -> names.getOrDefault(id, String.valueOf(id)))
                .limit(3)
                .collect(Collectors.joining(", "));
        if (ids.size() > 3) {
            joined += " 等" + ids.size() + "项";
        }
        return joined;
    }
}
