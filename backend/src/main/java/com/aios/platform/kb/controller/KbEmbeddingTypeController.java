package com.aios.platform.kb.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.common.dto.StringSelectOptionVO;
import com.aios.platform.kb.service.KbEmbeddingTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kb/embedding-types")
@RequiredArgsConstructor
public class KbEmbeddingTypeController {

    private final KbEmbeddingTypeService embeddingTypeService;

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('kb:base:view','kb:document:view')")
    public ApiResponse<List<StringSelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(embeddingTypeService.searchOptions(keyword, limit));
    }
}
