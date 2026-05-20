package com.aios.platform.kb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aios.vector")
public class VectorStoreProperties {

    private boolean enabled = true;
    private int dimensions = 1536;
    private Datasource datasource = new Datasource();

    @Data
    public static class Datasource {
        private String url = "jdbc:postgresql://127.0.0.1:5432/aios_vector";
        private String username = "aios";
        private String password = "aios123";
        private String driverClassName = "org.postgresql.Driver";
        private Hikari hikari = new Hikari();
    }

    @Data
    public static class Hikari {
        private int maximumPoolSize = 10;
        private int minimumIdle = 1;
        private long connectionTimeout = 30_000;
        private long validationTimeout = 5_000;
        private String connectionTestQuery = "SELECT 1";
    }
}
