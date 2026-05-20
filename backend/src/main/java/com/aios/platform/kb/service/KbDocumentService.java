package com.aios.platform.kb.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.dto.KbDocumentSaveOutcome;
import com.aios.platform.kb.dto.KbDocumentSaveRequest;
import com.aios.platform.kb.dto.KbDocumentVO;
import com.aios.platform.kb.entity.KbChunk;
import com.aios.platform.kb.entity.KbDocument;
import com.aios.platform.kb.entity.KbKnowledgeBase;
import com.aios.platform.kb.mapper.KbChunkMapper;
import com.aios.platform.kb.mapper.KbDocumentMapper;
import com.aios.platform.kb.mapper.KbKnowledgeBaseMapper;
import com.aios.platform.kb.service.KbFileStorageService.StoredFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentService {

    private static final Set<String> SOURCE_TYPES = Set.of("manual", "upload", "url");

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final KbKnowledgeBaseMapper kbMapper;
    private final KbFileStorageService fileStorageService;
    private final KbVectorRefService vectorRefService;
    private final ObjectProvider<KbVectorStoreService> vectorStoreService;
    private final ObjectProvider<KbDocumentIndexService> documentIndexService;

    public Page<KbDocumentVO> page(long current, long size, Long knowledgeBaseId, String keyword) {
        LambdaQueryWrapper<KbDocument> q = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            q.eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(KbDocument::getDocumentName, keyword)
                    .or()
                    .like(KbDocument::getSummary, keyword));
        }
        q.orderByDesc(KbDocument::getCreatedTime);
        Page<KbDocument> raw = documentMapper.selectPage(new Page<>(current, size), q);
        Map<Long, String> kbNames = loadKbNames(raw.getRecords());
        Page<KbDocumentVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(d -> toVo(d, kbNames)).toList());
        return out;
    }

    public KbDocumentVO get(Long id) {
        KbDocument d = requireDocument(id);
        Map<Long, String> kbNames = loadKbNames(List.of(d));
        return toVo(d, kbNames);
    }

    public java.nio.file.Path resolveFilePath(Long documentId) {
        KbDocument d = requireDocument(documentId);
        if (d.getFilePath() == null || d.getFilePath().isBlank()) {
            throw new BusinessException("该文档没有可下载的文件");
        }
        return fileStorageService.resolvePublic(d.getFilePath());
    }

    @Transactional
    public KbDocumentSaveOutcome save(KbDocumentSaveRequest req) {
        validateBase(req);
        String sourceType = normalizeSourceType(req.getSourceType());
        if (req.getDocumentId() != null) {
            KbDocument existing = requireDocument(req.getDocumentId());
            String existingType = existing.getSourceType() != null ? existing.getSourceType() : "manual";
            boolean urlChanged = "url".equals(sourceType)
                    && req.getSourceUrl() != null
                    && !req.getSourceUrl().isBlank()
                    && !req.getSourceUrl().equals(existing.getSourceUrl());
            if (sourceType.equals(existingType) && !urlChanged) {
                Long id = updateExisting(req, existing, sourceType);
                return new KbDocumentSaveOutcome(id, shouldReindexManual(existing, req));
            }
        }
        KbDocument doc = new KbDocument();
        doc.setDocumentId(req.getDocumentId());
        doc.setKnowledgeBaseId(req.getKnowledgeBaseId());
        doc.setDocumentName(req.getDocumentName().trim());
        doc.setSummary(req.getSummary());
        doc.setSourceType(sourceType);
        doc.setStatus(req.getStatus() != null ? req.getStatus() : 1);

        if ("manual".equals(sourceType)) {
            if (req.getContent() == null || req.getContent().isBlank()) {
                throw new BusinessException("请填写文档正文");
            }
            doc.setContent(req.getContent());
            doc.setFileType("text");
            doc.setSourceUrl(null);
        } else if ("url".equals(sourceType)) {
            StoredFile stored = fileStorageService.downloadFromUrl(req.getSourceUrl(), req.getKnowledgeBaseId(), req.getDocumentName());
            applyStoredFile(doc, stored, req.getSourceUrl());
        } else {
            throw new BusinessException("上传类型请使用上传接口");
        }

        Long id = persist(doc);
        return new KbDocumentSaveOutcome(id, true);
    }

    @Transactional
    public KbDocumentSaveOutcome saveUpload(
            Long knowledgeBaseId,
            String documentName,
            String summary,
            MultipartFile file,
            Long documentId) {
        if (knowledgeBaseId == null) {
            throw new BusinessException("请选择知识库");
        }
        requireKb(knowledgeBaseId);
        StoredFile stored = fileStorageService.storeUpload(file, knowledgeBaseId);
        KbDocument doc = new KbDocument();
        doc.setDocumentId(documentId);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setDocumentName(
                documentName != null && !documentName.isBlank() ? documentName.trim() : stored.originalName());
        doc.setSummary(summary);
        doc.setSourceType("upload");
        doc.setStatus(1);
        applyStoredFile(doc, stored, null);
        Long id = persist(doc);
        return new KbDocumentSaveOutcome(id, true);
    }

    /** 保存业务数据后按知识库配置同步向量化（供 Controller 在事务提交后调用）。 */
    public void vectorizeAfterSave(KbDocumentSaveOutcome outcome) {
        if (outcome == null || !outcome.vectorize() || outcome.documentId() == null) {
            return;
        }
        runAutoIndex(outcome.documentId());
    }

    @Transactional
    public void delete(Long id) {
        vectorStoreService.ifAvailable(v -> v.deleteByDocumentId(id));
        vectorRefService.deleteByDocumentId(id);
        chunkMapper.delete(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, id));
        documentMapper.deleteById(id);
    }

    public Map<Long, Long> countByKnowledgeBase() {
        List<KbDocument> all = documentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>().select(KbDocument::getKnowledgeBaseId));
        return all.stream()
                .filter(d -> d.getKnowledgeBaseId() != null)
                .collect(Collectors.groupingBy(KbDocument::getKnowledgeBaseId, Collectors.counting()));
    }

    private Long updateExisting(KbDocumentSaveRequest req, KbDocument db, String sourceType) {
        db.setKnowledgeBaseId(req.getKnowledgeBaseId());
        db.setDocumentName(req.getDocumentName().trim());
        db.setSummary(req.getSummary());
        if ("manual".equals(sourceType)) {
            if (req.getContent() == null || req.getContent().isBlank()) {
                throw new BusinessException("请填写文档正文");
            }
            db.setContent(req.getContent());
        }
        documentMapper.updateById(db);
        return db.getDocumentId();
    }

    private Long persist(KbDocument doc) {
        if (doc.getParseStatus() == null) {
            doc.setParseStatus("PENDING");
        }
        if (doc.getChunkCount() == null) {
            doc.setChunkCount(0);
        }
        if (doc.getDocumentId() == null) {
            documentMapper.insert(doc);
        } else {
            KbDocument db = requireDocument(doc.getDocumentId());
            if ("manual".equals(doc.getSourceType())) {
                db.setContent(doc.getContent());
                db.setFilePath(null);
                db.setFileUrl(null);
                db.setFileSize(0L);
                db.setSourceUrl(null);
            }
            db.setKnowledgeBaseId(doc.getKnowledgeBaseId());
            db.setDocumentName(doc.getDocumentName());
            db.setSummary(doc.getSummary());
            db.setSourceType(doc.getSourceType());
            db.setFileType(doc.getFileType());
            db.setParseStatus(doc.getParseStatus());
            db.setChunkCount(doc.getChunkCount());
            db.setStatus(doc.getStatus());
            if (!"manual".equals(doc.getSourceType()) && doc.getFilePath() != null) {
                db.setFilePath(doc.getFilePath());
                db.setFileUrl(doc.getFileUrl());
                db.setFileSize(doc.getFileSize());
                db.setSourceUrl(doc.getSourceUrl());
            }
            documentMapper.updateById(db);
            doc = db;
        }
        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            doc.setFileUrl("/api/kb/documents/" + doc.getDocumentId() + "/download");
            documentMapper.updateById(doc);
        }
        return doc.getDocumentId();
    }

    private void applyStoredFile(KbDocument doc, StoredFile stored, String sourceUrl) {
        String relative = fileStorageService.rootDir().relativize(stored.path().toAbsolutePath().normalize()).toString().replace('\\', '/');
        doc.setFilePath(relative);
        doc.setFileType(stored.extension());
        doc.setFileSize(stored.size());
        doc.setSourceUrl(sourceUrl);
        if ("txt".equals(stored.extension()) || "md".equals(stored.extension()) || "csv".equals(stored.extension())) {
            try {
                doc.setContent(Files.readString(stored.path(), StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                doc.setContent(null);
            }
        }
    }

    private void validateBase(KbDocumentSaveRequest req) {
        if (req.getKnowledgeBaseId() == null) {
            throw new BusinessException("请选择知识库");
        }
        requireKb(req.getKnowledgeBaseId());
        if (req.getDocumentName() == null || req.getDocumentName().isBlank()) {
            throw new BusinessException("文档名称不能为空");
        }
    }

    private String normalizeSourceType(String type) {
        String t = type != null ? type.trim().toLowerCase() : "manual";
        if (!SOURCE_TYPES.contains(t)) {
            throw new BusinessException("不支持的来源类型");
        }
        return t;
    }

    private KbKnowledgeBase requireKb(Long id) {
        KbKnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        return kb;
    }

    private KbDocument requireDocument(Long id) {
        KbDocument d = documentMapper.selectById(id);
        if (d == null) {
            throw new BusinessException("文档不存在");
        }
        return d;
    }

    private Map<Long, String> loadKbNames(List<KbDocument> docs) {
        Set<Long> ids = docs.stream().map(KbDocument::getKnowledgeBaseId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return kbMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(KbKnowledgeBase::getKnowledgeBaseId, KbKnowledgeBase::getKnowledgeBaseName, (a, b) -> a));
    }

    private KbDocumentVO toVo(KbDocument d, Map<Long, String> kbNames) {
        KbDocumentVO vo = new KbDocumentVO();
        vo.setDocumentId(d.getDocumentId());
        vo.setKnowledgeBaseId(d.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(kbNames.get(d.getKnowledgeBaseId()));
        vo.setDocumentName(d.getDocumentName());
        vo.setSummary(d.getSummary());
        vo.setSourceType(d.getSourceType());
        vo.setContent(d.getContent());
        vo.setSourceUrl(d.getSourceUrl());
        vo.setFileType(d.getFileType());
        vo.setFileUrl(d.getFileUrl());
        vo.setFilePath(d.getFilePath());
        vo.setFileSize(d.getFileSize());
        vo.setParseStatus(d.getParseStatus());
        vo.setChunkCount(d.getChunkCount());
        vo.setStatus(d.getStatus());
        vo.setCreatedTime(d.getCreatedTime());
        return vo;
    }

    private boolean shouldReindexManual(KbDocument existing, KbDocumentSaveRequest req) {
        if (!"manual".equals(existing.getSourceType())) {
            return false;
        }
        if (req.getContent() == null) {
            return false;
        }
        String before = existing.getContent() != null ? existing.getContent() : "";
        return !before.equals(req.getContent());
    }

    /**
     * 文档业务数据保存完成后，按知识库 Embedding 配置自动切块、写入 pgvector，并维护 kb_vector_ref 映射。
     */
    private void runAutoIndex(Long documentId) {
        documentIndexService.ifAvailable(indexer -> {
            try {
                indexer.indexDocument(documentId);
            } catch (Exception ex) {
                log.warn("文档 {} 自动向量化失败: {}", documentId, ex.getMessage());
            }
        });
    }
}
