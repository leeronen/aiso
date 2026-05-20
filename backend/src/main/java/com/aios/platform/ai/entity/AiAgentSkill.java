package com.aios.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_agent_skill")
public class AiAgentSkill {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private Long skillId;
    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
