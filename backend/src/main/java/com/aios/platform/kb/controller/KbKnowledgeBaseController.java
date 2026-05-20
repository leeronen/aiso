package com.aios.platform.kb.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.common.dto.SelectOptionVO;
import com.aios.platform.kb.dto.KbBaseTreeNode;
import com.aios.platform.kb.dto.KbKnowledgeBaseVO;
import com.aios.platform.kb.entity.KbKnowledgeBase;
import com.aios.platform.kb.service.KbDocumentService;
import com.aios.platform.kb.service.KbKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kb/bases")
@RequiredArgsConstructor
public class KbKnowledgeBaseController {

    private final KbKnowledgeBaseService kbService;
    private final KbDocumentService documentService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<List<KbBaseTreeNode>> tree() {
        return ApiResponse.ok(kbService.buildTree(documentService.countByKnowledgeBase()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('kb:base:view')")
    public ApiResponse<Page<KbKnowledgeBaseVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(kbService.page(current, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('kb:base:view','ai:agent:view')")
    public ApiResponse<List<SelectOptionVO>> options(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(kbService.searchOptions(keyword, limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:base:view')")
    public ApiResponse<KbKnowledgeBaseVO> get(@PathVariable Long id) {
        return ApiResponse.ok(kbService.getVo(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('kb:base:add','kb:base:update')")
    public ApiResponse<java.util.Map<String, Long>> save(@RequestBody KbKnowledgeBase body) {
        Long id = kbService.save(body);
        return ApiResponse.ok(java.util.Map.of("knowledgeBaseId", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:base:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        kbService.delete(id);
        return ApiResponse.ok();
    }
}
