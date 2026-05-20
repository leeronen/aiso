package com.aios.platform.kb.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_vector_ref")
public class KbVectorRef extends BaseEntity {

    @TableId(value = "ref_id", type = IdType.AUTO)
    private Long refId;

    /** pgvector 表 kb_chunk_vector.vector_id */
    private Long vectorId;

    /** 业务库 kb_document.document_id */
    private Long documentId;

    /** 业务库 kb_chunk.chunk_id */
    private Long chunkId;

    private Long knowledgeBaseId;
    private String vectorStore;
    private String embeddingModel;
    private Integer status;
}
