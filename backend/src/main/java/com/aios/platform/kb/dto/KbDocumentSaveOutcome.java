package com.aios.platform.kb.dto;

/** 文档保存结果：业务 ID + 是否需要在保存后执行向量化。 */
public record KbDocumentSaveOutcome(Long documentId, boolean vectorize) {}
