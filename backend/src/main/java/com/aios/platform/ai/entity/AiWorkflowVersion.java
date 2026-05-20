package com.aios.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_workflow_version")
public class AiWorkflowVersion {

    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    private Long workflowId;
    private Integer versionNo;
    private String workflowName;
    private String description;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private String graphJson;
    private String dslJson;
    private String stepsJson;
    private String changeSummary;
    private LocalDateTime createdTime;
    private Long createdUserId;
    private Integer deleted;
}
