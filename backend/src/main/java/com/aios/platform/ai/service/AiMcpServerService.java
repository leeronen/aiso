package com.aios.platform.ai.service;

import com.aios.platform.ai.dto.AiMcpServerVO;
import com.aios.platform.ai.entity.AiMcpServer;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.ai.mapper.AiMcpServerMapper;
import com.aios.platform.ai.mapper.AiSkillMapper;
import com.aios.platform.ai.support.IoSchemaSupport;
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMcpServerService {

    private final AiMcpServerMapper mcpServerMapper;
    private final AiSkillMapper skillMapper;

    public Page<AiMcpServerVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AiMcpServer> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiMcpServer::getServerName, keyword)
                    .or()
                    .like(AiMcpServer::getProtocolType, keyword)
                    .or()
                    .like(AiMcpServer::getServerUrl, keyword));
        }
        q.orderByDesc(AiMcpServer::getCreatedTime);
        Page<AiMcpServer> raw = mcpServerMapper.selectPage(new Page<>(current, size), q);
        Page<AiMcpServerVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(AiMcpServerService::toVo).toList());
        return out;
    }

    public List<AiMcpServerVO> listEnabled() {
        return listEnabled(null);
    }

    public List<AiMcpServerVO> listEnabled(String keyword) {
        LambdaQueryWrapper<AiMcpServer> q = new LambdaQueryWrapper<AiMcpServer>().eq(AiMcpServer::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiMcpServer::getServerName, keyword)
                    .or()
                    .like(AiMcpServer::getProtocolType, keyword)
                    .or()
                    .like(AiMcpServer::getServerUrl, keyword));
        }
        q.orderByAsc(AiMcpServer::getServerName).last("LIMIT 100");
        return mcpServerMapper.selectList(q).stream().map(AiMcpServerService::toVo).toList();
    }

    public List<SelectOptionVO> searchOptions(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AiMcpServer> q = new LambdaQueryWrapper<AiMcpServer>().eq(AiMcpServer::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(AiMcpServer::getServerName, keyword)
                    .or()
                    .like(AiMcpServer::getProtocolType, keyword));
        }
        q.orderByAsc(AiMcpServer::getServerName).last("LIMIT " + size);
        return mcpServerMapper.selectList(q).stream()
                .map(m -> new SelectOptionVO(m.getMcpServerId(), m.getServerName()))
                .toList();
    }

    public AiMcpServerVO get(Long id) {
        AiMcpServer row = mcpServerMapper.selectById(id);
        if (row == null) {
            throw new BusinessException("MCP 服务不存在");
        }
        return toVo(row);
    }

    @Transactional
    public Long save(AiMcpServer body) {
        IoSchemaSupport.validate(body.getInputType(), body.getInputSchema(), "输入");
        IoSchemaSupport.validate(body.getOutputType(), body.getOutputSchema(), "输出");
        if (body.getMcpServerId() == null) {
            if (body.getStatus() == null) {
                body.setStatus(1);
            }
            if (body.getInputType() == null || body.getInputType().isBlank()) {
                body.setInputType("object");
            }
            if (body.getOutputType() == null || body.getOutputType().isBlank()) {
                body.setOutputType("object");
            }
            mcpServerMapper.insert(body);
            return body.getMcpServerId();
        }
        AiMcpServer db = mcpServerMapper.selectById(body.getMcpServerId());
        if (db == null) {
            throw new BusinessException("MCP 服务不存在");
        }
        db.setServerName(body.getServerName());
        db.setProtocolType(body.getProtocolType());
        db.setServerUrl(body.getServerUrl());
        db.setAuthConfig(body.getAuthConfig());
        db.setInputType(body.getInputType());
        db.setOutputType(body.getOutputType());
        db.setInputSchema(body.getInputSchema());
        db.setOutputSchema(body.getOutputSchema());
        db.setStatus(body.getStatus());
        mcpServerMapper.updateById(db);
        return db.getMcpServerId();
    }

    @Transactional
    public void delete(Long id) {
        Long skillRef = skillMapper.selectCount(
                new LambdaQueryWrapper<AiSkill>().eq(AiSkill::getMcpServerId, id));
        if (skillRef != null && skillRef > 0) {
            throw new BusinessException("该 MCP 已被 Skill 引用，请先解除关联");
        }
        mcpServerMapper.deleteById(id);
    }

    private static AiMcpServerVO toVo(AiMcpServer r) {
        AiMcpServerVO vo = new AiMcpServerVO();
        vo.setMcpServerId(r.getMcpServerId());
        vo.setServerName(r.getServerName());
        vo.setProtocolType(r.getProtocolType());
        vo.setServerUrl(r.getServerUrl());
        vo.setAuthConfig(r.getAuthConfig());
        vo.setInputType(r.getInputType());
        vo.setOutputType(r.getOutputType());
        vo.setInputSchema(r.getInputSchema());
        vo.setOutputSchema(r.getOutputSchema());
        vo.setStatus(r.getStatus());
        vo.setCreatedTime(r.getCreatedTime());
        return vo;
    }
}
