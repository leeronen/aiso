package com.aios.platform.ai.controller;

import com.aios.platform.ai.dto.AiWorkflowSaveRequest;
import com.aios.platform.ai.dto.AiWorkflowVO;
import com.aios.platform.ai.dto.AiWorkflowVersionDetailVO;
import com.aios.platform.ai.dto.AiWorkflowVersionVO;
import com.aios.platform.ai.service.AiWorkflowService;
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
@RequestMapping("/api/ai/workflows")
@RequiredArgsConstructor
public class AiWorkflowController {

    private final AiWorkflowService workflowService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:workflow:view')")
    public ApiResponse<Page<AiWorkflowVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(workflowService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('ai:workflow:view','ai:skill:view','invoke:workflow:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(workflowService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:workflow:view')")
    public ApiResponse<AiWorkflowVO> get(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ai:workflow:add','ai:workflow:update')")
    public ApiResponse<Map<String, Long>> save(@RequestBody AiWorkflowSaveRequest body) {
        Long id = workflowService.save(body);
        return ApiResponse.ok(Map.of("workflowId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:workflow:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('ai:workflow:view')")
    public ApiResponse<List<AiWorkflowVersionVO>> versions(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.listVersions(id));
    }

    @GetMapping("/versions/{versionId}")
    @PreAuthorize("hasAuthority('ai:workflow:view')")
    public ApiResponse<AiWorkflowVersionDetailVO> versionDetail(@PathVariable Long versionId) {
        return ApiResponse.ok(workflowService.getVersion(versionId));
    }

    @PostMapping("/versions/{versionId}/restore")
    @PreAuthorize("hasAuthority('ai:workflow:update')")
    public ApiResponse<Map<String, Long>> restore(@PathVariable Long versionId) {
        Long workflowId = workflowService.restoreVersion(versionId);
        return ApiResponse.ok(Map.of("workflowId", workflowId));
    }
}
