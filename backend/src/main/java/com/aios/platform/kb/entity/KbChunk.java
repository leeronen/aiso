package com.aios.platform.kb.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_chunk")
public class KbChunk extends BaseEntity {

    @TableId(value = "chunk_id", type = IdType.AUTO)
    private Long chunkId;

    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private String metadataJson;
    private Integer status;
}
