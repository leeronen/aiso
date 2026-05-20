package com.aios.platform.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public abstract class BaseEntity implements Serializable {

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField(value = "created_user_id", fill = FieldFill.INSERT)
    private Long createdUserId;

    @TableField(value = "updated_user_id", fill = FieldFill.INSERT_UPDATE)
    private Long updatedUserId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
