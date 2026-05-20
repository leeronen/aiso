package com.aios.platform.kb.controller;

import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.kb.dto.KbDocumentSaveRequest;
import com.aios.platform.kb.dto.KbDocumentVO;
import com.aios.platform.kb.dto.KbVectorRefVO;
import com.aios.platform.kb.service.KbVectorRefService;
import com.aios.platform.kb.service.KbDocumentIndexService;
import com.aios.platform.kb.service.KbDocumentService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kb/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService documentService;
    private final ObjectProvider<KbDocumentIndexService> documentIndexService;
    private final KbVectorRefService vectorRefService;

    @GetMapping
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<Page<KbDocumentVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(documentService.page(current, size, knowledgeBaseId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<KbDocumentVO> get(@PathVariable Long id) {
        return ApiResponse.ok(documentService.get(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var path = documentService.resolveFilePath(id);
        KbDocumentVO meta = documentService.get(id);
        String filename = meta.getDocumentName() != null ? meta.getDocumentName() : "document";
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('kb:document:add','kb:document:update')")
    public ApiResponse<Map<String, Object>> save(@RequestBody KbDocumentSaveRequest body) {
        var outcome = documentService.save(body);
        documentService.vectorizeAfterSave(outcome);
        return ApiResponse.ok(buildSaveResult(outcome.documentId()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('kb:document:add','kb:document:update')")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam Long knowledgeBaseId,
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) Long documentId,
            @RequestParam("file") MultipartFile file) {
        var outcome = documentService.saveUpload(knowledgeBaseId, documentName, summary, file, documentId);
        documentService.vectorizeAfterSave(outcome);
        return ApiResponse.ok(buildSaveResult(outcome.documentId()));
    }

    private Map<String, Object> buildSaveResult(Long documentId) {
        KbDocumentVO doc = documentService.get(documentId);
        return Map.of(
                "documentId",
                documentId,
                "chunkCount",
                doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                "parseStatus",
                doc.getParseStatus() != null ? doc.getParseStatus() : "PENDING");
    }

    @GetMapping("/{id}/vector-refs")
    @PreAuthorize("hasAuthority('kb:document:view')")
    public ApiResponse<List<KbVectorRefVO>> vectorRefs(@PathVariable Long id) {
        return ApiResponse.ok(vectorRefService.listByDocumentId(id));
    }

    @PostMapping("/{id}/index")
    @PreAuthorize("hasAuthority('kb:document:update')")
    public ApiResponse<Map<String, Object>> index(@PathVariable Long id) {
        KbDocumentIndexService indexer = documentIndexService.getIfAvailable();
        if (indexer == null) {
            return ApiResponse.fail(503, "pgvector 向量库未启用，请检查 aios.vector.enabled 与 PostgreSQL 连接");
        }
        int chunkCount = indexer.indexDocument(id);
        return ApiResponse.ok(Map.of("chunkCount", chunkCount, "parseStatus", "DONE"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('kb:document:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok();
    }
}
