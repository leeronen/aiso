package com.aios.platform.system.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.system.entity.SysPermission;
import com.aios.platform.system.service.SysPermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('permission:view')")
    public ApiResponse<Page<SysPermission>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(permissionService.page(current, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:view')")
    public ApiResponse<SysPermission> get(@PathVariable Long id) {
        return ApiResponse.ok(permissionService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:add')")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysPermission body) {
        Long id = permissionService.save(body);
        return ApiResponse.ok(Map.of("permissionId", id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    public ApiResponse<Map<String, Long>> update(@PathVariable Long id, @RequestBody SysPermission body) {
        body.setPermissionId(id);
        Long pid = permissionService.save(body);
        return ApiResponse.ok(Map.of("permissionId", pid));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.ok();
    }
}
