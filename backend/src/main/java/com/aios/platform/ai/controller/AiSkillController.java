package com.aios.platform.ai.controller;

import com.aios.platform.ai.dto.AiSkillVO;
import com.aios.platform.ai.entity.AiSkill;
import com.aios.platform.ai.service.AiSkillService;
import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.common.dto.SelectOptionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/skills")
@RequiredArgsConstructor
public class AiSkillController {

    private final AiSkillService skillService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:skill:view')")
    public ApiResponse<Page<AiSkillVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(skillService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('ai:skill:view','ai:agent:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(skillService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:skill:view')")
    public ApiResponse<AiSkillVO> get(@PathVariable Long id) {
        return ApiResponse.ok(skillService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ai:skill:add','ai:skill:update')")
    public ApiResponse<Map<String, Long>> save(@RequestBody AiSkill body) {
        Long id = skillService.save(body);
        return ApiResponse.ok(Map.of("skillId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:skill:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skillService.delete(id);
        return ApiResponse.ok();
    }
}
