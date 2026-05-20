package com.aios.platform.chat.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;
    private String roleType;
    private String content;
    private Integer tokenCount;
    private Long responseTime;
}
