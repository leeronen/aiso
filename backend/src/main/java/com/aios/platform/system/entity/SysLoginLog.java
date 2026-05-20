package com.aios.platform.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private String message;
    private LocalDateTime createdTime;
}
