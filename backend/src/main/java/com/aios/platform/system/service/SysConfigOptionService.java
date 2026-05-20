package com.aios.platform.system.service;

import com.aios.platform.system.dto.ConfigOptionVO;
import com.aios.platform.system.entity.SysConfigOption;
import com.aios.platform.system.mapper.SysConfigOptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysConfigOptionService {

    public static final String AGENT_MEMORY_MODE = "agent_memory_mode";
    public static final String AGENT_TOOL_MODE = "agent_tool_mode";

    private final SysConfigOptionMapper configOptionMapper;

    public List<ConfigOptionVO> listByType(String configType) {
        List<SysConfigOption> rows = configOptionMapper.selectList(
                new LambdaQueryWrapper<SysConfigOption>()
                        .eq(SysConfigOption::getConfigType, configType)
                        .eq(SysConfigOption::getStatus, 1)
                        .orderByAsc(SysConfigOption::getSortOrder)
                        .orderByAsc(SysConfigOption::getOptionId));
        return rows.stream()
                .map(r -> {
                    ConfigOptionVO vo = new ConfigOptionVO();
                    vo.setValue(r.getConfigCode());
                    vo.setLabel(r.getConfigLabel());
                    vo.setDescription(r.getDescription());
                    return vo;
                })
                .toList();
    }

    public String labelOf(String configType, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        SysConfigOption row = configOptionMapper.selectOne(
                new LambdaQueryWrapper<SysConfigOption>()
                        .eq(SysConfigOption::getConfigType, configType)
                        .eq(SysConfigOption::getConfigCode, code)
                        .last("LIMIT 1"));
        return row != null ? row.getConfigLabel() : code;
    }

    public void validateCode(String configType, String code, String fieldName) {
        if (code == null || code.isBlank()) {
            return;
        }
        Long count = configOptionMapper.selectCount(
                new LambdaQueryWrapper<SysConfigOption>()
                        .eq(SysConfigOption::getConfigType, configType)
                        .eq(SysConfigOption::getConfigCode, code)
                        .eq(SysConfigOption::getStatus, 1));
        if (count == 0) {
            throw new com.aios.platform.common.exception.BusinessException(fieldName + "无效: " + code);
        }
    }
}
