package com.aios.platform.kb.entity;

import com.aios.platform.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class KbDocument extends BaseEntity {

    @TableId(value = "document_id", type = IdType.AUTO)
    private Long documentId;

    private Long knowledgeBaseId;
    private String documentName;
    private String summary;
    private String sourceType;
    private String content;
    private String sourceUrl;
    private String fileType;
    private String fileUrl;
    private String filePath;
    private Long fileSize;
    private String parseStatus;
    private Integer chunkCount;
    private Integer status;
}
