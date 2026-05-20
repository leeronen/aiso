package com.aios.platform.kb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aios.embedding")
public class EmbeddingProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "text-embedding-3-small";
    private int dimensions = 1536;
}
