package com.aios.platform.system.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.system.dto.SysUserVO;
import com.aios.platform.system.dto.UserSaveRequest;
import com.aios.platform.system.service.SysUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<Page<SysUserVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userService.page(current, size, keyword));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<SysUserVO> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getDetail(id));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public ApiResponse<Map<String, Long>> create(@RequestBody UserSaveRequest body) {
        Long id = userService.create(body);
        return ApiResponse.ok(Map.of("userId", id));
    }

    @Operation(summary = "编辑用户（昵称、状态、是否管理员等）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UserSaveRequest body) {
        userService.update(id, body);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }

    public record PasswordBody(@NotBlank String password) {}

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<Void> password(@PathVariable Long id, @Valid @RequestBody PasswordBody body) {
        userService.resetPassword(id, body.password());
        return ApiResponse.ok();
    }
}
