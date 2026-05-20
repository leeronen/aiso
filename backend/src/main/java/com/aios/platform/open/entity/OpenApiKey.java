package com.aios.platform.open.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_key")
public class OpenApiKey extends BaseEntity {

    @TableId(value = "api_key_id", type = IdType.AUTO)
    private Long apiKeyId;

    private String appName;
    private String apiKey;
    private String apiSecret;
    private Integer status;
}
