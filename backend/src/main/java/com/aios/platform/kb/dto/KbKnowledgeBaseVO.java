package com.aios.platform.kb.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KbKnowledgeBaseVO {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private String description;
    private String embeddingTypeCode;
    private String embeddingTypeName;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer overlapSize;
    private Integer status;
    private LocalDateTime createdTime;
}
