package com.aios.platform.runtime;

import com.aios.platform.ai.entity.AiModel;
import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.common.trace.TraceIds;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LlmChatClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${aios.llm.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${aios.embedding.api-key:}")
    private String defaultApiKey;

    public LlmChatResult chat(AiModel model, List<LlmMessage> messages) {
        if (model == null) {
            throw new BusinessException("模型配置不存在");
        }
        String apiKey = resolveApiKey(model);
        if (apiKey.isBlank()) {
            throw new BusinessException("未配置大模型 API Key（模型或环境变量 OPENAI_API_KEY）");
        }
        String baseUrl = model.getBaseUrl() != null && !model.getBaseUrl().isBlank()
                ? model.getBaseUrl().replaceAll("/+$", "")
                : "https://api.openai.com/v1";
        String modelCode = model.getModelCode() != null && !model.getModelCode().isBlank()
                ? model.getModelCode()
                : "gpt-4o-mini";

        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", modelCode);
            ArrayNode msgArr = body.putArray("messages");
            for (LlmMessage m : messages) {
                ObjectNode o = msgArr.addObject();
                o.put("role", m.role());
                o.put("content", m.content());
            }
            if (model.getMaxTokens() != null && model.getMaxTokens() > 0) {
                body.put("max_tokens", model.getMaxTokens());
            }
            applyDecimal(body, "temperature", model.getTemperature(), 0.7);
            applyDecimal(body, "top_p", model.getTopP(), 1.0);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey);
            String traceId = TraceIds.current();
            if (traceId != null && !traceId.isBlank()) {
                builder.header(TraceIds.HEADER, traceId);
            }
            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("LLM HTTP {} body={}", response.statusCode(), truncate(response.body(), 500));
                throw new BusinessException("大模型调用失败: HTTP " + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new BusinessException("大模型响应格式异常");
            }
            String text = content.asText();
            LlmTokenUsage usage = parseUsage(root.get("usage"));
            if (usage.totalTokens() <= 0) {
                usage = TokenEstimator.estimateFromMessages(messages, text);
            }
            return new LlmChatResult(text, usage);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed model={}", modelCode, e);
            throw new BusinessException("大模型调用失败: " + e.getMessage());
        }
    }

    private String resolveApiKey(AiModel model) {
        if (model.getApiKey() != null && !model.getApiKey().isBlank()) {
            return model.getApiKey();
        }
        return defaultApiKey != null ? defaultApiKey : "";
    }

    private static void applyDecimal(ObjectNode body, String field, BigDecimal value, double defaultVal) {
        double v = value != null ? value.doubleValue() : defaultVal;
        body.put(field, v);
    }

    private static LlmTokenUsage parseUsage(JsonNode usage) {
        if (usage == null || usage.isMissingNode()) {
            return LlmTokenUsage.empty();
        }
        int prompt = usage.path("prompt_tokens").asInt(0);
        int completion = usage.path("completion_tokens").asInt(0);
        int total = usage.path("total_tokens").asInt(0);
        if (total <= 0 && (prompt > 0 || completion > 0)) {
            total = prompt + completion;
        }
        return new LlmTokenUsage(prompt, completion, total);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
