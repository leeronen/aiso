package com.aios.platform.ai.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent")
public class AiAgent extends BaseEntity {

    @TableId(value = "agent_id", type = IdType.AUTO)
    private Long agentId;

    private String agentName;
    private String description;
    private String avatar;
    private Long modelId;
    private String systemPrompt;
    private String welcomeMessage;
    private String thinkingMode;
    private String memoryMode;
    private String toolMode;
    private Integer memoryEnabled;
    private Integer knowledgeEnabled;
    private Integer toolEnabled;
    private Integer status;
}
