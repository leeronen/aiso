package com.aios.platform.kb.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KbDocumentVO {

    private Long documentId;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
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
    private LocalDateTime createdTime;
}
