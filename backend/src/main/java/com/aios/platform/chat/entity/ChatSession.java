package com.aios.platform.chat.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    private Long userId;
    private Long agentId;
    private Long workflowId;
    private String sessionTitle;
    private String sessionStatus;
}
