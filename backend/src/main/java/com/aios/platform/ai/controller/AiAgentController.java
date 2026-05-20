package com.aios.platform.ai.controller;

import com.aios.platform.ai.dto.AiAgentSaveRequest;
import com.aios.platform.ai.dto.AiAgentVO;
import com.aios.platform.ai.service.AiAgentService;
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
@RequestMapping("/api/ai/agents")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService agentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:agent:view')")
    public ApiResponse<Page<AiAgentVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(agentService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('ai:agent:view','ai:workflow:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(agentService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:agent:view')")
    public ApiResponse<AiAgentVO> get(@PathVariable Long id) {
        return ApiResponse.ok(agentService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ai:agent:add','ai:agent:update')")
    public ApiResponse<Map<String, Long>> save(@RequestBody AiAgentSaveRequest body) {
        Long id = agentService.save(body);
        return ApiResponse.ok(Map.of("agentId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:agent:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return ApiResponse.ok();
    }
}
