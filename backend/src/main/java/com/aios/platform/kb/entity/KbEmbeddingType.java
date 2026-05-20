package com.aios.platform.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("kb_embedding_type")
public class KbEmbeddingType {

    @TableId(value = "embedding_type_id", type = IdType.AUTO)
    private Long embeddingTypeId;

    private String typeCode;
    private String typeName;
    private String description;
    private String granularity;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
