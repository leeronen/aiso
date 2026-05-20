package com.aios.platform.invoke.service;

import com.aios.platform.ai.entity.AiWorkflow;
import com.aios.platform.ai.mapper.AiWorkflowMapper;
import com.aios.platform.ai.service.AiTokenUsageService;
import com.aios.platform.ai.support.WorkflowIoSupport;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.invoke.dto.WorkflowIoMetaVO;
import com.aios.platform.invoke.dto.WorkflowInvokeResultVO;
import com.aios.platform.runtime.TokenUsageContext;
import com.aios.platform.runtime.WorkflowRuntimeService;
import com.aios.platform.runtime.dto.ChatExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowInvokeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiWorkflowMapper workflowMapper;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final AiTokenUsageService tokenUsageService;

    public WorkflowIoMetaVO ioMeta(Long workflowId) {
        AiWorkflow w = requireWorkflow(workflowId);
        WorkflowIoMetaVO vo = new WorkflowIoMetaVO();
        vo.setWorkflowId(w.getWorkflowId());
        vo.setWorkflowName(w.getWorkflowName());
        vo.setInputType(w.getInputType());
        vo.setOutputType(w.getOutputType());
        vo.setInputSchema(w.getInputSchema());
        vo.setOutputSchema(w.getOutputSchema());
        vo.setExampleInput(
                WorkflowIoSupport.exampleInput(w.getInputType(), w.getInputSchema()));
        return vo;
    }

    public WorkflowInvokeResultVO invoke(Long workflowId, JsonNode input, Long userId) {
        AiWorkflow w = requireWorkflow(workflowId);
        WorkflowIoSupport.validateInput(w.getInputType(), w.getInputSchema(), input);
        String prompt = WorkflowIoSupport.toPromptMessage(w.getInputType(), input);

        long t0 = System.currentTimeMillis();
        ChatExecutionResult execution =
                workflowRuntimeService.executeWorkflow(w, prompt, Collections.emptyList(), false);
        long elapsed = System.currentTimeMillis() - t0;

        TokenUsageContext ctx =
                TokenUsageContext.builder().userId(userId).workflowId(workflowId).build();
        tokenUsageService.recordBatch(ctx, execution.getUsageRecords());

        WorkflowInvokeResultVO vo = new WorkflowInvokeResultVO();
        vo.setWorkflowId(w.getWorkflowId());
        vo.setWorkflowName(w.getWorkflowName());
        vo.setInputType(w.getInputType());
        vo.setOutputType(w.getOutputType());
        vo.setInput(MAPPER.convertValue(input, Object.class));
        vo.setOutput(WorkflowIoSupport.formatOutput(w.getOutputType(), w.getOutputSchema(), execution));
        vo.setRawReply(execution.getReply());
        vo.setSteps(execution.getSteps());
        vo.setPromptTokens(execution.getPromptTokens());
        vo.setCompletionTokens(execution.getCompletionTokens());
        vo.setTotalTokens(execution.getTotalTokens());
        vo.setElapsedMs(elapsed);
        return vo;
    }

    private AiWorkflow requireWorkflow(Long workflowId) {
        AiWorkflow w = workflowMapper.selectById(workflowId);
        if (w == null) {
            throw new BusinessException("工作流不存在");
        }
        if (w.getStatus() != null && w.getStatus() == 0) {
            throw new BusinessException("工作流已停用");
        }
        if (w.getDslJson() == null || w.getDslJson().isBlank()) {
            throw new BusinessException("工作流未配置画布，请先保存工作流");
        }
        return w;
    }
}
