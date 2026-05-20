package com.aios.platform.kb.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.common.trace.TraceIds;
import com.aios.platform.kb.config.EmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EmbeddingProperties properties;

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (!properties.isEnabled()) {
            return texts.stream().map(t -> zeroVector()).toList();
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("未配置 aios.embedding.api-key，使用零向量占位（仅开发调试）");
            return texts.stream().map(t -> zeroVector()).toList();
        }
        try {
            String url = properties.getBaseUrl().replaceAll("/+$", "") + "/embeddings";
            var body = MAPPER.createObjectNode();
            body.put("model", properties.getModel());
            var input = body.putArray("input");
            texts.forEach(input::add);
            if (properties.getDimensions() > 0) {
                body.put("dimensions", properties.getDimensions());
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey());
            String traceId = TraceIds.current();
            if (traceId != null && !traceId.isBlank()) {
                builder.header(TraceIds.HEADER, traceId);
            }
            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Embedding API 失败: HTTP " + response.statusCode() + " " + response.body());
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                throw new BusinessException("Embedding API 响应格式异常");
            }
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode emb = item.get("embedding");
                if (emb == null || !emb.isArray()) {
                    throw new BusinessException("Embedding 数据缺失");
                }
                float[] vec = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) {
                    vec[i] = (float) emb.get(i).asDouble();
                }
                vectors.add(vec);
            }
            return vectors;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("调用 Embedding 失败: " + e.getMessage());
        }
    }

    public float[] embedOne(String text) {
        List<float[]> list = embedBatch(List.of(text));
        return list.isEmpty() ? zeroVector() : list.get(0);
    }

    public String modelName() {
        return properties.getModel();
    }

    public int dimensions() {
        return properties.getDimensions();
    }

    private float[] zeroVector() {
        float[] v = new float[properties.getDimensions()];
        return v;
    }
}
