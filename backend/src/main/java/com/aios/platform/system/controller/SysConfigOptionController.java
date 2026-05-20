package com.aios.platform.system.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.system.dto.ConfigOptionVO;
import com.aios.platform.system.service.SysConfigOptionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/config-options")
@RequiredArgsConstructor
public class SysConfigOptionController {

    private final SysConfigOptionService configOptionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ai:agent:view')")
    public ApiResponse<List<ConfigOptionVO>> list(@RequestParam String configType) {
        return ApiResponse.ok(configOptionService.listByType(configType));
    }
}
