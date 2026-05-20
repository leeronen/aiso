package com.aios.platform.open.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.invoke.dto.WorkflowIoMetaVO;
import com.aios.platform.invoke.dto.WorkflowInvokeResultVO;
import com.aios.platform.invoke.service.WorkflowInvokeService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 对外开放的工作流调用（Header: X-API-Key）。 */
@RestController
@RequestMapping("/open/v1/workflows")
@RequiredArgsConstructor
public class OpenWorkflowInvokeController {

    private final WorkflowInvokeService invokeService;

    @GetMapping("/{workflowId}/io")
    public ApiResponse<WorkflowIoMetaVO> ioMeta(@PathVariable Long workflowId) {
        return ApiResponse.ok(invokeService.ioMeta(workflowId));
    }

    @PostMapping("/{workflowId}/invoke")
    public ApiResponse<WorkflowInvokeResultVO> invoke(
            @PathVariable Long workflowId, @RequestBody(required = false) JsonNode body) {
        return ApiResponse.ok(invokeService.invoke(workflowId, body, null));
    }
}
