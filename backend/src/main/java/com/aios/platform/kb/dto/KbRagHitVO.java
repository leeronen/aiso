package com.aios.platform.kb.dto;

import lombok.Data;

@Data
public class KbRagHitVO {

    private Long vectorId;
    private Long chunkId;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Double score;
}
