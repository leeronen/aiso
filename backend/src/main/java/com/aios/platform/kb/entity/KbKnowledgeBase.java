package com.aios.platform.kb.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_knowledge_base")
public class KbKnowledgeBase extends BaseEntity {

    @TableId(value = "knowledge_base_id", type = IdType.AUTO)
    private Long knowledgeBaseId;

    private String knowledgeBaseName;
    private String description;
    private String embeddingTypeCode;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer overlapSize;
    private Integer status;
}
