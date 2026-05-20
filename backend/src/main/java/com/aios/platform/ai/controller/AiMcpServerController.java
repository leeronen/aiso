package com.aios.platform.ai.controller;

import com.aios.platform.ai.dto.AiMcpServerVO;
import com.aios.platform.ai.entity.AiMcpServer;
import com.aios.platform.ai.service.AiMcpServerService;
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
@RequestMapping("/api/ai/mcp-servers")
@RequiredArgsConstructor
public class AiMcpServerController {

    private final AiMcpServerService mcpServerService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:mcp:view')")
    public ApiResponse<Page<AiMcpServerVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(mcpServerService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('ai:mcp:view','ai:skill:view','ai:agent:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(mcpServerService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:mcp:view')")
    public ApiResponse<AiMcpServerVO> get(@PathVariable Long id) {
        return ApiResponse.ok(mcpServerService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ai:mcp:add','ai:mcp:update')")
    public ApiResponse<Map<String, Long>> save(@RequestBody AiMcpServer body) {
        Long id = mcpServerService.save(body);
        return ApiResponse.ok(Map.of("mcpServerId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:mcp:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        mcpServerService.delete(id);
        return ApiResponse.ok();
    }
}
