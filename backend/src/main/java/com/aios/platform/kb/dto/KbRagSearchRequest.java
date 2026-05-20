package com.aios.platform.kb.dto;

import lombok.Data;

@Data
public class KbRagSearchRequest {

    private Long knowledgeBaseId;
    private String query;
    private Integer topK = 5;
}
