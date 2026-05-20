package com.aios.platform.ai.controller;

import com.aios.platform.ai.entity.AiModel;
import com.aios.platform.ai.service.AiModelService;
import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.common.dto.SelectOptionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
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
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService modelService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:model:view')")
    public ApiResponse<Page<AiModel>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(modelService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('ai:model:view','ai:agent:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(modelService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:model:view')")
    public ApiResponse<AiModel> get(@PathVariable Long id) {
        return ApiResponse.ok(modelService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ai:model:add','ai:model:update')")
    public ApiResponse<java.util.Map<String, Long>> save(@RequestBody AiModel body) {
        Long id = modelService.save(body);
        return ApiResponse.ok(java.util.Map.of("modelId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:model:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return ApiResponse.ok();
    }
}
