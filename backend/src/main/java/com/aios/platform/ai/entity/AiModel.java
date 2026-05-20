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
@TableName("ai_model")
public class AiModel extends BaseEntity {

    @TableId(value = "model_id", type = IdType.AUTO)
    private Long modelId;

    private String modelName;
    private String modelCode;
    private String providerType;
    private String baseUrl;
    private String apiKey;
    private Integer maxTokens;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer supportFunctionCall;
    private Integer supportVision;
    private Integer supportEmbedding;
    private Integer status;
    private String remark;
}
