package com.aios.platform.ai.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_skill")
public class AiSkill extends BaseEntity {

    @TableId(value = "skill_id", type = IdType.AUTO)
    private Long skillId;

    private String skillName;
    private String description;
    private Long promptTemplateId;
    private Long workflowId;
    private Long mcpServerId;
    private Long inputModelId;
    private Long outputModelId;
    private String inputType;
    private String outputType;
    private String inputSchema;
    private String outputSchema;
    private Integer status;
}
