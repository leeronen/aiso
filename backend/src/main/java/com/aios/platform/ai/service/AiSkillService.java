package com.aios.platform.ai.service;

import com.aios.platform.ai.dto.AiSkillVO;
import com.aios.platform.ai.entity.AiMcpServer;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.ai.mapper.AiMcpServerMapper;
import com.aios.platform.ai.mapper.AiSkillMapper;
import com.aios.platform.ai.support.IoSchemaSupport;
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashMap;
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
public class AiSkillService {

    private final AiSkillMapper skillMapper;
    private final AiMcpServerMapper mcpServerMapper;

    public Page<AiSkillVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AiSkill> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiSkill::getSkillName, keyword).or().like(AiSkill::getDescription, keyword));
        }
        q.orderByDesc(AiSkill::getCreatedTime);
        Page<AiSkill> raw = skillMapper.selectPage(new Page<>(current, size), q);
        Map<Long, String> mcpNames = loadMcpNames(raw.getRecords());
        Page<AiSkillVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(r -> toVo(r, mcpNames)).toList());
        return out;
    }

    public AiSkillVO get(Long id) {
        AiSkill row = skillMapper.selectById(id);
        if (row == null) {
            throw new BusinessException("Skill 不存在");
        }
        Map<Long, String> mcpNames = loadMcpNames(List.of(row));
        return toVo(row, mcpNames);
    }

    @Transactional
    public Long save(AiSkill body) {
        IoSchemaSupport.validate(body.getInputType(), body.getInputSchema(), "输入");
        IoSchemaSupport.validate(body.getOutputType(), body.getOutputSchema(), "输出");
        if (body.getMcpServerId() != null && mcpServerMapper.selectById(body.getMcpServerId()) == null) {
            throw new BusinessException("关联 MCP 不存在");
        }
        if (body.getSkillId() == null) {
            if (body.getStatus() == null) {
                body.setStatus(1);
            }
            if (body.getInputType() == null || body.getInputType().isBlank()) {
                body.setInputType("object");
            }
            if (body.getOutputType() == null || body.getOutputType().isBlank()) {
                body.setOutputType("object");
            }
            skillMapper.insert(body);
            return body.getSkillId();
        }
        AiSkill db = skillMapper.selectById(body.getSkillId());
        if (db == null) {
            throw new BusinessException("Skill 不存在");
        }
        db.setSkillName(body.getSkillName());
        db.setDescription(body.getDescription());
        db.setPromptTemplateId(body.getPromptTemplateId());
        db.setWorkflowId(body.getWorkflowId());
        db.setMcpServerId(body.getMcpServerId());
        db.setInputType(body.getInputType());
        db.setOutputType(body.getOutputType());
        db.setInputSchema(body.getInputSchema());
        db.setOutputSchema(body.getOutputSchema());
        db.setStatus(body.getStatus());
        skillMapper.updateById(db);
        return db.getSkillId();
    }

    @Transactional
    public void delete(Long id) {
        skillMapper.deleteById(id);
    }

    private Map<Long, String> loadMcpNames(List<AiSkill> rows) {
        Set<Long> ids = rows.stream()
                .map(AiSkill::getMcpServerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<AiMcpServer> mcps = mcpServerMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (AiMcpServer m : mcps) {
            map.put(m.getMcpServerId(), m.getServerName());
        }
        return map;
    }

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AiSkill> q = new LambdaQueryWrapper<AiSkill>().eq(AiSkill::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiSkill::getSkillName, keyword).or().like(AiSkill::getDescription, keyword));
        }
        q.orderByAsc(AiSkill::getSkillName).last("LIMIT " + size);
        return skillMapper.selectList(q).stream()
                .map(s -> new SelectOptionVO(s.getSkillId(), s.getSkillName()))
                .toList();
    }

    private static AiSkillVO toVo(AiSkill r, Map<Long, String> mcpNames) {
        AiSkillVO vo = new AiSkillVO();
        vo.setSkillId(r.getSkillId());
        vo.setSkillName(r.getSkillName());
        vo.setDescription(r.getDescription());
        vo.setPromptTemplateId(r.getPromptTemplateId());
        vo.setWorkflowId(r.getWorkflowId());
        vo.setMcpServerId(r.getMcpServerId());
        vo.setMcpServerName(mcpNames.get(r.getMcpServerId()));
        vo.setInputType(r.getInputType());
        vo.setOutputType(r.getOutputType());
        vo.setInputSchema(r.getInputSchema());
        vo.setOutputSchema(r.getOutputSchema());
        vo.setStatus(r.getStatus());
        vo.setCreatedTime(r.getCreatedTime());
        return vo;
    }
}
