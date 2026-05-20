package com.aios.platform.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_config_option")
public class SysConfigOption {

    @TableId(value = "option_id", type = IdType.AUTO)
    private Long optionId;

    private String configType;
    private String configCode;
    private String configLabel;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
