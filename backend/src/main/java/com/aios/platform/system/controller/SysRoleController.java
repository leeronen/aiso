package com.aios.platform.system.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.system.dto.RoleSaveRequest;
import com.aios.platform.system.dto.SysRoleVO;
import com.aios.platform.system.entity.SysRole;
import com.aios.platform.system.service.SysRoleService;
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

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<Page<SysRole>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(roleService.page(current, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<SysRoleVO> get(@PathVariable Long id) {
        return ApiResponse.ok(roleService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public ApiResponse<Map<String, Long>> create(@RequestBody RoleSaveRequest body) {
        Long id = roleService.create(body);
        return ApiResponse.ok(Map.of("roleId", id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ApiResponse<Map<String, Long>> update(@PathVariable Long id, @RequestBody RoleSaveRequest body) {
        roleService.update(id, body);
        return ApiResponse.ok(Map.of("roleId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }
}
