package com.aios.platform.invoke.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.invoke.dto.WorkflowIoMetaVO;
import com.aios.platform.invoke.dto.WorkflowInvokeResultVO;
import com.aios.platform.invoke.service.WorkflowInvokeService;
import com.aios.platform.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoke/workflows")
@RequiredArgsConstructor
public class WorkflowInvokeController {

    private final WorkflowInvokeService invokeService;

    @GetMapping("/{workflowId}/io")
    @PreAuthorize("hasAuthority('invoke:workflow:view')")
    public ApiResponse<WorkflowIoMetaVO> ioMeta(@PathVariable Long workflowId) {
        return ApiResponse.ok(invokeService.ioMeta(workflowId));
    }

    @PostMapping("/{workflowId}")
    @PreAuthorize("hasAuthority('invoke:workflow:run')")
    public ApiResponse<WorkflowInvokeResultVO> invoke(
            @PathVariable Long workflowId, @RequestBody(required = false) JsonNode body) {
        return ApiResponse.ok(invokeService.invoke(workflowId, body, currentUserId()));
    }

    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getUserId();
        }
        throw new BusinessException(401, "未登录");
    }
}
