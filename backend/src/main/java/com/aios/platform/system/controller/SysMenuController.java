package com.aios.platform.system.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.system.entity.SysMenu;
import com.aios.platform.system.service.AuthorizationService;
import com.aios.platform.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;
    private final AuthorizationService authorizationService;

    @GetMapping("/nav")
    public ApiResponse<List<SysMenu>> nav() {
        Set<String> codes = authorizationService.currentPermissionCodes();
        boolean admin = authorizationService.currentIsSuperAdmin();
        return ApiResponse.ok(menuService.navTree(codes, admin));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('menu:view')")
    public ApiResponse<List<SysMenu>> tree() {
        return ApiResponse.ok(menuService.treeForManage());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:view')")
    public ApiResponse<SysMenu> get(@PathVariable Long id) {
        return ApiResponse.ok(menuService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:add')")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysMenu body) {
        Long id = menuService.save(body);
        return ApiResponse.ok(Map.of("menuId", id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:update')")
    public ApiResponse<Map<String, Long>> update(@PathVariable Long id, @RequestBody SysMenu body) {
        body.setMenuId(id);
        Long mid = menuService.save(body);
        return ApiResponse.ok(Map.of("menuId", mid));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }
}
