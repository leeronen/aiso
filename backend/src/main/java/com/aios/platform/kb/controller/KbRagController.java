package com.aios.platform.kb.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.kb.dto.KbRagHitVO;
import com.aios.platform.kb.dto.KbRagSearchRequest;
import com.aios.platform.kb.service.KbVectorStoreService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kb/rag")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aios.vector.enabled", havingValue = "true", matchIfMissing = true)
public class KbRagController {

    private final KbVectorStoreService vectorStoreService;

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<List<KbRagHitVO>> search(@RequestBody KbRagSearchRequest body) {
        if (body.getKnowledgeBaseId() == null) {
            return ApiResponse.fail(400, "请指定知识库");
        }
        int topK = body.getTopK() != null ? body.getTopK() : 5;
        return ApiResponse.ok(vectorStoreService.search(body.getKnowledgeBaseId(), body.getQuery(), topK));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<Map<String, Long>> stats(@RequestParam Long knowledgeBaseId) {
        return ApiResponse.ok(Map.of("vectorCount", vectorStoreService.countByKnowledgeBase(knowledgeBaseId)));
    }
}
