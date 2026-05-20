package com.aios.platform.kb.dto;

import lombok.Data;

@Data
public class KbDocumentSaveRequest {

    private Long documentId;
    private Long knowledgeBaseId;
    private String documentName;
    private String summary;
    /** manual | upload | url */
    private String sourceType;
    private String content;
    private String sourceUrl;
    private String fileType;
    private Integer status;
}
