package com.aios.platform.ai.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_workflow")
public class AiWorkflow extends BaseEntity {

    @TableId(value = "workflow_id", type = IdType.AUTO)
    private Long workflowId;

    private String workflowName;
    private String description;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private String executionMode;
    private String dslJson;
    private Integer status;
    /** 展示用版本标签，如 v3 */
    private String version;
    /** 当前版本号（每次保存递增） */
    private Integer versionNo;
}
