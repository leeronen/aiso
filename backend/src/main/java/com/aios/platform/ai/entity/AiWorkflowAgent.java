package com.aios.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_workflow_agent")
public class AiWorkflowAgent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;
    private Long agentId;
    private Integer sortOrder;
    private String nodeKey;
    private String nodeLabel;
    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
