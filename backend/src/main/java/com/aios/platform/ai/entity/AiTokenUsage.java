package com.aios.platform.ai.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_token_usage")
public class AiTokenUsage extends BaseEntity {

    @TableId(value = "usage_id", type = IdType.AUTO)
    private Long usageId;

    private Long userId;
    private Long agentId;
    private Long sessionId;
    private Long workflowId;
    private Long messageId;
    private String usageType;
    private String modelCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal cost;
}
