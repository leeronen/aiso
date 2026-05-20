package com.aios.platform.ai.service;

import com.aios.platform.ai.dto.AiWorkflowSaveRequest;
import com.aios.platform.ai.dto.AiWorkflowVO;
import com.aios.platform.ai.dto.AiWorkflowVersionDetailVO;
import com.aios.platform.ai.dto.AiWorkflowVersionVO;
import com.aios.platform.ai.dto.WorkflowStepDTO;
import com.aios.platform.ai.entity.AiAgent;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.ai.entity.AiWorkflow;
import com.aios.platform.ai.entity.AiWorkflowAgent;
import com.aios.platform.ai.entity.AiWorkflowVersion;
import com.aios.platform.ai.mapper.AiAgentMapper;
import com.aios.platform.ai.mapper.AiSkillMapper;
import com.aios.platform.ai.mapper.AiWorkflowAgentMapper;
import com.aios.platform.ai.mapper.AiWorkflowMapper;
import com.aios.platform.ai.mapper.AiWorkflowVersionMapper;
import com.aios.platform.ai.support.IoSchemaSupport;
import com.aios.platform.ai.support.WorkflowDslSupport;
import com.aios.platform.ai.support.WorkflowGraphSupport;
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
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
public class AiWorkflowService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiWorkflowMapper workflowMapper;
    private final AiWorkflowAgentMapper workflowAgentMapper;
    private final AiWorkflowVersionMapper workflowVersionMapper;
    private final AiAgentMapper agentMapper;
    private final AiSkillMapper skillMapper;

    public Page<AiWorkflowVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AiWorkflow> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiWorkflow::getWorkflowName, keyword).or().like(AiWorkflow::getDescription, keyword));
        }
        q.orderByDesc(AiWorkflow::getCreatedTime);
        Page<AiWorkflow> raw = workflowMapper.selectPage(new Page<>(current, size), q);
        Map<Long, List<WorkflowStepDTO>> stepMap = loadSteps(raw.getRecords());
        Map<Long, String> agentNames = loadAgentNames(stepMap);
        Page<AiWorkflowVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream()
                .map(w -> toVo(w, stepMap, agentNames, false))
                .toList());
        return out;
    }

    public AiWorkflowVO get(Long id) {
        AiWorkflow w = requireWorkflow(id);
        Map<Long, List<WorkflowStepDTO>> stepMap = loadSteps(List.of(w));
        Map<Long, String> agentNames = loadAgentNames(stepMap);
        return toVo(w, stepMap, agentNames, true);
    }

    public List<AiWorkflowVersionVO> listVersions(Long workflowId) {
        requireWorkflow(workflowId);
        List<AiWorkflowVersion> rows =
                workflowVersionMapper.selectList(
                        new LambdaQueryWrapper<AiWorkflowVersion>()
                                .eq(AiWorkflowVersion::getWorkflowId, workflowId)
                                .orderByDesc(AiWorkflowVersion::getVersionNo));
        return rows.stream().map(this::toVersionVo).toList();
    }

    public AiWorkflowVersionDetailVO getVersion(Long versionId) {
        AiWorkflowVersion v = workflowVersionMapper.selectById(versionId);
        if (v == null) {
            throw new BusinessException("版本记录不存在");
        }
        return toVersionDetail(v);
    }

    @Transactional
    public Long restoreVersion(Long versionId) {
        AiWorkflowVersion snap = workflowVersionMapper.selectById(versionId);
        if (snap == null) {
            throw new BusinessException("版本记录不存在");
        }
        AiWorkflowSaveRequest req = new AiWorkflowSaveRequest();
        req.setWorkflowId(snap.getWorkflowId());
        req.setWorkflowName(snap.getWorkflowName());
        req.setDescription(snap.getDescription());
        req.setInputType(snap.getInputType());
        req.setOutputType(snap.getOutputType());
        req.setInputSchema(snap.getInputSchema());
        req.setOutputSchema(snap.getOutputSchema());
        req.setGraphJson(snap.getGraphJson());
        req.setStatus(1);
        try {
            if (snap.getStepsJson() != null && !snap.getStepsJson().isBlank()) {
                req.setSteps(MAPPER.readValue(snap.getStepsJson(), new TypeReference<List<WorkflowStepDTO>>() {}));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("版本步骤数据损坏");
        }
        String summary = "从 " + WorkflowDslSupport.versionLabel(snap.getVersionNo()) + " 恢复";
        return save(req, summary);
    }

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AiWorkflow> q = new LambdaQueryWrapper<AiWorkflow>().eq(AiWorkflow::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiWorkflow::getWorkflowName, keyword).or().like(AiWorkflow::getDescription, keyword));
        }
        q.orderByAsc(AiWorkflow::getWorkflowName).last("LIMIT " + size);
        return workflowMapper.selectList(q).stream()
                .map(w -> new SelectOptionVO(w.getWorkflowId(), w.getWorkflowName()))
                .toList();
    }

    @Transactional
    public Long save(AiWorkflowSaveRequest req) {
        return save(req, null);
    }

    @Transactional
    public Long save(AiWorkflowSaveRequest req, String changeSummary) {
        if (req.getWorkflowName() == null || req.getWorkflowName().isBlank()) {
            throw new BusinessException("工作流名称不能为空");
        }
        String inputType = req.getInputType() != null ? req.getInputType() : "object";
        String outputType = req.getOutputType() != null ? req.getOutputType() : "object";
        IoSchemaSupport.validate(inputType, req.getInputSchema(), "入参");
        IoSchemaSupport.validate(outputType, req.getOutputSchema(), "出参");

        WorkflowGraphSupport.ResolvedGraph resolved =
                WorkflowGraphSupport.resolve(req.getGraphJson(), req.getSteps());
        List<WorkflowStepDTO> steps = normalizeSteps(resolved.steps());
        if (steps.isEmpty()) {
            throw new BusinessException("请至少配置一个 Agent 节点");
        }
        validateAgents(steps);

        boolean isNew = req.getWorkflowId() == null;
        int versionNo;
        if (isNew) {
            versionNo = 1;
        } else {
            AiWorkflow db = requireWorkflow(req.getWorkflowId());
            versionNo = (db.getVersionNo() != null ? db.getVersionNo() : 0) + 1;
        }

        String dslJson;
        try {
            dslJson = WorkflowDslSupport.buildDsl(
                    versionNo,
                    inputType,
                    outputType,
                    req.getInputSchema(),
                    req.getOutputSchema(),
                    steps,
                    resolved.graphJson(),
                    resolved.executionLayers());
        } catch (JsonProcessingException e) {
            throw new BusinessException("生成工作流 DSL 失败");
        }

        AiWorkflow body = new AiWorkflow();
        body.setWorkflowId(req.getWorkflowId());
        body.setWorkflowName(req.getWorkflowName());
        body.setDescription(req.getDescription());
        body.setInputType(inputType);
        body.setOutputType(outputType);
        body.setInputSchema(req.getInputSchema());
        body.setOutputSchema(req.getOutputSchema());
        body.setExecutionMode("graph");
        body.setDslJson(dslJson);
        body.setVersionNo(versionNo);
        body.setVersion(WorkflowDslSupport.versionLabel(versionNo));
        body.setStatus(req.getStatus() != null ? req.getStatus() : 1);

        Long workflowId;
        if (isNew) {
            workflowMapper.insert(body);
            workflowId = body.getWorkflowId();
        } else {
            AiWorkflow db = requireWorkflow(body.getWorkflowId());
            db.setWorkflowName(body.getWorkflowName());
            db.setDescription(body.getDescription());
            db.setInputType(body.getInputType());
            db.setOutputType(body.getOutputType());
            db.setInputSchema(body.getInputSchema());
            db.setOutputSchema(body.getOutputSchema());
            db.setExecutionMode(body.getExecutionMode());
            db.setDslJson(body.getDslJson());
            db.setVersionNo(body.getVersionNo());
            db.setVersion(body.getVersion());
            db.setStatus(body.getStatus());
            workflowMapper.updateById(db);
            workflowId = db.getWorkflowId();
            body = db;
        }

        replaceSteps(workflowId, steps);
        saveVersionSnapshot(body, resolved.graphJson(), steps, changeSummary, isNew);
        return workflowId;
    }

    @Transactional
    public void delete(Long id) {
        Long skillRef =
                skillMapper.selectCount(new LambdaQueryWrapper<AiSkill>().eq(AiSkill::getWorkflowId, id));
        if (skillRef != null && skillRef > 0) {
            throw new BusinessException("该工作流已被 Skill 引用，请先解除关联");
        }
        workflowAgentMapper.delete(
                new LambdaQueryWrapper<AiWorkflowAgent>().eq(AiWorkflowAgent::getWorkflowId, id));
        workflowVersionMapper.delete(
                new LambdaQueryWrapper<AiWorkflowVersion>().eq(AiWorkflowVersion::getWorkflowId, id));
        workflowMapper.deleteById(id);
    }

    private void saveVersionSnapshot(
            AiWorkflow w,
            String graphJson,
            List<WorkflowStepDTO> steps,
            String changeSummary,
            boolean isNew) {
        AiWorkflowVersion snap = new AiWorkflowVersion();
        snap.setWorkflowId(w.getWorkflowId());
        snap.setVersionNo(w.getVersionNo());
        snap.setWorkflowName(w.getWorkflowName());
        snap.setDescription(w.getDescription());
        snap.setInputType(w.getInputType());
        snap.setOutputType(w.getOutputType());
        snap.setInputSchema(w.getInputSchema());
        snap.setOutputSchema(w.getOutputSchema());
        snap.setGraphJson(graphJson);
        snap.setDslJson(w.getDslJson());
        try {
            snap.setStepsJson(MAPPER.writeValueAsString(steps));
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存版本快照失败");
        }
        if (changeSummary != null && !changeSummary.isBlank()) {
            snap.setChangeSummary(changeSummary);
        } else {
            snap.setChangeSummary(isNew ? "初始版本" : "更新配置");
        }
        workflowVersionMapper.insert(snap);
    }

    private AiWorkflow requireWorkflow(Long id) {
        AiWorkflow w = workflowMapper.selectById(id);
        if (w == null) {
            throw new BusinessException("工作流不存在");
        }
        return w;
    }

    private void validateAgents(List<WorkflowStepDTO> steps) {
        Set<Long> agentIds =
                steps.stream().map(WorkflowStepDTO::getAgentId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (agentIds.size() != steps.size()) {
            throw new BusinessException("Agent 节点配置不完整");
        }
        List<AiAgent> agents = agentMapper.selectBatchIds(agentIds);
        if (agents.size() != agentIds.size()) {
            throw new BusinessException("存在无效的 Agent");
        }
        for (AiAgent a : agents) {
            if (a.getStatus() != null && a.getStatus() == 0) {
                throw new BusinessException("Agent「" + a.getAgentName() + "」已停用，无法加入工作流");
            }
        }
    }

    private List<WorkflowStepDTO> normalizeSteps(List<WorkflowStepDTO> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<WorkflowStepDTO> out = new ArrayList<>();
        int order = 1;
        for (WorkflowStepDTO step : raw) {
            if (step == null || step.getAgentId() == null) {
                continue;
            }
            WorkflowStepDTO n = new WorkflowStepDTO();
            n.setAgentId(step.getAgentId());
            n.setSortOrder(step.getSortOrder() != null ? step.getSortOrder() : order);
            String key = step.getNodeKey();
            if (key == null || key.isBlank()) {
                key = "node_" + order;
            }
            n.setNodeKey(key.trim());
            n.setNodeLabel(step.getNodeLabel() != null ? step.getNodeLabel().trim() : null);
            out.add(n);
            order++;
        }
        Set<String> keys = out.stream().map(WorkflowStepDTO::getNodeKey).collect(Collectors.toSet());
        if (keys.size() != out.size()) {
            throw new BusinessException("节点标识 nodeKey 不能重复");
        }
        Set<Long> agents = out.stream().map(WorkflowStepDTO::getAgentId).collect(Collectors.toSet());
        if (agents.size() != out.size()) {
            throw new BusinessException("同一工作流中 Agent 不能重复");
        }
        return out;
    }

    private void replaceSteps(Long workflowId, List<WorkflowStepDTO> steps) {
        workflowAgentMapper.delete(
                new LambdaQueryWrapper<AiWorkflowAgent>().eq(AiWorkflowAgent::getWorkflowId, workflowId));
        for (WorkflowStepDTO step : steps) {
            AiWorkflowAgent rel = new AiWorkflowAgent();
            rel.setWorkflowId(workflowId);
            rel.setAgentId(step.getAgentId());
            rel.setSortOrder(step.getSortOrder());
            rel.setNodeKey(step.getNodeKey());
            rel.setNodeLabel(step.getNodeLabel());
            workflowAgentMapper.insert(rel);
        }
    }

    private Map<Long, List<WorkflowStepDTO>> loadSteps(List<AiWorkflow> workflows) {
        if (workflows.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = workflows.stream().map(AiWorkflow::getWorkflowId).toList();
        List<AiWorkflowAgent> rels =
                workflowAgentMapper.selectList(
                        new LambdaQueryWrapper<AiWorkflowAgent>().in(AiWorkflowAgent::getWorkflowId, ids));
        return rels.stream()
                .collect(Collectors.groupingBy(
                        AiWorkflowAgent::getWorkflowId,
                        Collectors.collectingAndThen(Collectors.toList(), this::toStepDtos)));
    }

    private List<WorkflowStepDTO> toStepDtos(List<AiWorkflowAgent> rels) {
        return rels.stream()
                .sorted(Comparator.comparing(AiWorkflowAgent::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(rel -> {
                    WorkflowStepDTO dto = new WorkflowStepDTO();
                    dto.setAgentId(rel.getAgentId());
                    dto.setSortOrder(rel.getSortOrder());
                    dto.setNodeKey(rel.getNodeKey());
                    dto.setNodeLabel(rel.getNodeLabel());
                    return dto;
                })
                .toList();
    }

    private Map<Long, String> loadAgentNames(Map<Long, List<WorkflowStepDTO>> stepMap) {
        Set<Long> ids =
                stepMap.values().stream()
                        .flatMap(List::stream)
                        .map(WorkflowStepDTO::getAgentId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return agentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AiAgent::getAgentId, AiAgent::getAgentName, (a, b) -> a));
    }

    private AiWorkflowVO toVo(
            AiWorkflow w, Map<Long, List<WorkflowStepDTO>> stepMap, Map<Long, String> agentNames, boolean withSteps) {
        AiWorkflowVO vo = new AiWorkflowVO();
        vo.setWorkflowId(w.getWorkflowId());
        vo.setWorkflowName(w.getWorkflowName());
        vo.setDescription(w.getDescription());
        vo.setInputType(w.getInputType());
        vo.setOutputType(w.getOutputType());
        vo.setInputSchema(w.getInputSchema());
        vo.setOutputSchema(w.getOutputSchema());
        vo.setDslJson(w.getDslJson());
        vo.setVersion(w.getVersion());
        vo.setVersionNo(w.getVersionNo());
        vo.setStatus(w.getStatus());
        vo.setCreatedTime(w.getCreatedTime());

        List<WorkflowStepDTO> steps = stepMap.getOrDefault(w.getWorkflowId(), List.of());
        vo.setAgentCount(steps.size());
        vo.setAgentSummary(summarizeAgents(steps, agentNames));
        if (withSteps) {
            vo.setSteps(steps);
            vo.setGraphJson(WorkflowGraphSupport.extractGraphJson(w.getDslJson(), steps));
        }
        return vo;
    }

    private AiWorkflowVersionVO toVersionVo(AiWorkflowVersion v) {
        AiWorkflowVersionVO vo = new AiWorkflowVersionVO();
        vo.setVersionId(v.getVersionId());
        vo.setWorkflowId(v.getWorkflowId());
        vo.setVersionNo(v.getVersionNo());
        vo.setVersionLabel(WorkflowDslSupport.versionLabel(v.getVersionNo() != null ? v.getVersionNo() : 1));
        vo.setWorkflowName(v.getWorkflowName());
        vo.setChangeSummary(v.getChangeSummary());
        vo.setCreatedTime(v.getCreatedTime());
        try {
            if (v.getStepsJson() != null && !v.getStepsJson().isBlank()) {
                List<WorkflowStepDTO> steps =
                        MAPPER.readValue(v.getStepsJson(), new TypeReference<List<WorkflowStepDTO>>() {});
                vo.setAgentCount(steps.size());
            }
        } catch (Exception ignored) {
            vo.setAgentCount(0);
        }
        return vo;
    }

    private AiWorkflowVersionDetailVO toVersionDetail(AiWorkflowVersion v) {
        AiWorkflowVersionDetailVO vo = new AiWorkflowVersionDetailVO();
        vo.setVersionId(v.getVersionId());
        vo.setWorkflowId(v.getWorkflowId());
        vo.setVersionNo(v.getVersionNo());
        vo.setVersionLabel(WorkflowDslSupport.versionLabel(v.getVersionNo() != null ? v.getVersionNo() : 1));
        vo.setWorkflowName(v.getWorkflowName());
        vo.setDescription(v.getDescription());
        vo.setInputType(v.getInputType());
        vo.setOutputType(v.getOutputType());
        vo.setInputSchema(v.getInputSchema());
        vo.setOutputSchema(v.getOutputSchema());
        vo.setGraphJson(v.getGraphJson());
        vo.setDslJson(v.getDslJson());
        vo.setChangeSummary(v.getChangeSummary());
        vo.setCreatedTime(v.getCreatedTime());
        try {
            if (v.getStepsJson() != null && !v.getStepsJson().isBlank()) {
                vo.setSteps(MAPPER.readValue(v.getStepsJson(), new TypeReference<List<WorkflowStepDTO>>() {}));
            }
        } catch (JsonProcessingException e) {
            vo.setSteps(List.of());
        }
        return vo;
    }

    private String summarizeAgents(List<WorkflowStepDTO> steps, Map<Long, String> agentNames) {
        if (steps.isEmpty()) {
            return "—";
        }
        String joined = steps.stream()
                .map(s -> agentNames.getOrDefault(s.getAgentId(), String.valueOf(s.getAgentId())))
                .limit(3)
                .collect(Collectors.joining(" → "));
        if (steps.size() > 3) {
            joined += " 等" + steps.size() + "个节点";
        }
        return joined;
    }
}
