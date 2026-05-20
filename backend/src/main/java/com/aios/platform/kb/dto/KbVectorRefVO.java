package com.aios.platform.kb.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KbVectorRefVO {

    private Long refId;
    private Long vectorId;
    private Long documentId;
    private Long chunkId;
    private Long knowledgeBaseId;
    private String vectorStore;
    private String embeddingModel;
    private Integer status;
    private LocalDateTime createdTime;
}
