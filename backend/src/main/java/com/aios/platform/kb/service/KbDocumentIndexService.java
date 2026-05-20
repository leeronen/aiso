package com.aios.platform.kb.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.entity.KbChunk;
import com.aios.platform.kb.entity.KbDocument;
import com.aios.platform.kb.entity.KbKnowledgeBase;
import com.aios.platform.kb.mapper.KbChunkMapper;
import com.aios.platform.kb.mapper.KbDocumentMapper;
import com.aios.platform.kb.support.EmbeddingChunkStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aios.vector.enabled", havingValue = "true", matchIfMissing = true)
public class KbDocumentIndexService {

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final KbKnowledgeBaseService knowledgeBaseService;
    private final KbFileStorageService fileStorageService;
    private final EmbeddingService embeddingService;
    private final KbVectorStoreService vectorStoreService;
    private final KbVectorRefService vectorRefService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 不在长事务中执行：Embedding HTTP 调用可能耗时数十秒，若加 @Transactional 会长时间占用 MySQL 连接导致连接池耗尽。
     */
    public int indexDocument(Long documentId) {
        KbDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        if (doc.getKnowledgeBaseId() == null) {
            throw new BusinessException("文档未关联知识库");
        }
        KbKnowledgeBase kb = knowledgeBaseService.get(doc.getKnowledgeBaseId());

        markParseStatus(documentId, "INDEXING", null);

        try {
            String text = loadDocumentText(doc);
            if (text.isBlank()) {
                throw new BusinessException("文档无可用正文，无法向量化");
            }

            removeChunksForDocument(documentId);

            List<String> pieces = EmbeddingChunkStrategy.split(kb, text);
            if (pieces.isEmpty()) {
                throw new BusinessException("分块结果为空");
            }

            String model = resolveEmbeddingModel(kb);
            int batchSize = 16;
            int total = 0;
            for (int i = 0; i < pieces.size(); i += batchSize) {
                List<String> batch = pieces.subList(i, Math.min(i + batchSize, pieces.size()));
                List<float[]> vectors = embeddingService.embedBatch(batch);
                total += persistChunkBatch(doc, documentId, model, batch, vectors, i);
            }

            markParseStatus(documentId, "DONE", total);
            log.info("文档 {} 已向量化，共 {} 个 chunk", documentId, total);
            return total;
        } catch (Exception e) {
            try {
                removeChunksForDocument(documentId);
            } catch (Exception cleanupEx) {
                log.warn("向量化失败后清理 chunk 异常: {}", cleanupEx.getMessage());
            }
            markParseStatus(documentId, "FAILED", null);
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException("向量化失败: " + resolveErrorMessage(e));
        }
    }

    private void markParseStatus(Long documentId, String parseStatus, Integer chunkCount) {
        transactionTemplate.executeWithoutResult(tx -> {
            KbDocument patch = new KbDocument();
            patch.setDocumentId(documentId);
            patch.setParseStatus(parseStatus);
            if (chunkCount != null) {
                patch.setChunkCount(chunkCount);
            }
            documentMapper.updateById(patch);
        });
    }

    private int persistChunkBatch(
            KbDocument doc,
            Long documentId,
            String model,
            List<String> batch,
            List<float[]> vectors,
            int indexOffset) {
        Integer saved = transactionTemplate.execute(status -> {
            int count = 0;
            for (int j = 0; j < batch.size(); j++) {
                int chunkIndex = indexOffset + j;
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(chunkIndex);
                chunk.setContent(batch.get(j));
                chunk.setStatus(1);
                chunkMapper.insert(chunk);

                long vectorId = vectorStoreService.upsert(
                        chunk.getChunkId(),
                        doc.getKnowledgeBaseId(),
                        documentId,
                        chunkIndex,
                        batch.get(j),
                        vectors.get(j),
                        model);
                vectorRefService.saveOrUpdate(
                        vectorId, documentId, chunk.getChunkId(), doc.getKnowledgeBaseId(), model);
                count++;
            }
            return count;
        });
        return saved != null ? saved : 0;
    }

    private static String resolveErrorMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (root instanceof SQLException sqlEx && sqlEx.getSQLState() != null) {
            msg = msg + " (SQLState=" + sqlEx.getSQLState() + ")";
        }
        if (msg != null && msg.contains("Failed to obtain JDBC Connection")) {
            boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
            return msg
                    + (inTx ? " [MySQL 事务中]" : "")
                    + "。请检查: 1) pgvector 容器是否运行; 2) aios.vector.datasource.url 是否指向 PostgreSQL(5432); 3) 连接池是否耗尽";
        }
        if (root instanceof java.net.UnknownHostException && "127.0.0.1".equals(msg)) {
            return "无法连接本地 pgvector（127.0.0.1:5432）。"
                    + "常见原因：JVM 配置了 SOCKS 代理 socksProxyHost=127.0.0.1，"
                    + "请关闭 IDE/终端代理或为本地地址设置 socksNonProxyHosts；"
                    + "并确认已执行: docker compose -f docker/docker-compose.yml up -d pgvector";
        }
        return msg != null ? msg : e.getClass().getSimpleName();
    }

    private String resolveEmbeddingModel(KbKnowledgeBase kb) {
        if (kb.getEmbeddingModel() != null && !kb.getEmbeddingModel().isBlank()) {
            return kb.getEmbeddingModel();
        }
        return embeddingService.modelName();
    }

    private void removeChunksForDocument(Long documentId) {
        List<KbChunk> old = chunkMapper.selectList(
                new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, documentId));
        List<Long> ids = old.stream().map(KbChunk::getChunkId).toList();
        vectorStoreService.deleteByDocumentId(documentId);
        vectorRefService.deleteByDocumentId(documentId);
        if (!ids.isEmpty()) {
            chunkMapper.deleteBatchIds(ids);
        }
    }

    private String loadDocumentText(KbDocument doc) {
        if (doc.getContent() != null && !doc.getContent().isBlank()) {
            return doc.getContent();
        }
        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            try {
                var path = fileStorageService.resolvePublic(doc.getFilePath());
                String type = doc.getFileType() != null ? doc.getFileType().toLowerCase() : "";
                if (type.equals("txt") || type.equals("md") || type.equals("csv") || type.equals("json") || type.equals("html") || type.equals("htm")) {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
                throw new BusinessException("当前仅支持向量化文本类文件 (txt/md/csv/json/html)，PDF 等需后续接入解析器");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("读取文件失败: " + e.getMessage());
            }
        }
        return "";
    }
}
