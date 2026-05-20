package com.aios.platform.kb.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** pgvector 专用数据源，非 @Primary，避免抢占 MySQL 主库。 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aios.vector.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorDataSourceConfig {

    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource(VectorStoreProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        VectorStoreProperties.Datasource cfg = properties.getDatasource();
        ds.setJdbcUrl(withJdbcParams(cfg.getUrl()));
        ds.setUsername(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        ds.setDriverClassName(cfg.getDriverClassName());
        VectorStoreProperties.Hikari hk = cfg.getHikari();
        ds.setPoolName("aios-pgvector");
        ds.setMaximumPoolSize(hk.getMaximumPoolSize());
        ds.setMinimumIdle(hk.getMinimumIdle());
        ds.setConnectionTimeout(hk.getConnectionTimeout());
        ds.setValidationTimeout(hk.getValidationTimeout());
        ds.setIdleTimeout(300_000);
        ds.setMaxLifetime(600_000);
        ds.setKeepaliveTime(60_000);
        ds.setConnectionTestQuery(hk.getConnectionTestQuery());
        // 避免 pgvector 不可达时阻塞应用启动超过 1 分钟
        ds.setInitializationFailTimeout(5_000);
        warmUp(ds);
        return ds;
    }

    private static String withJdbcParams(String url) {
        if (url == null || url.isBlank()) {
            return "jdbc:postgresql://127.0.0.1:5432/aios_vector?connectTimeout=10&socketTimeout=120&tcpKeepAlive=true";
        }
        if (url.contains("connectTimeout=")) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "connectTimeout=10&socketTimeout=120&tcpKeepAlive=true";
    }

    private static void warmUp(HikariDataSource ds) {
        try (Connection ignored = ds.getConnection()) {
            log.info("pgvector 连接池预热成功: {}", ds.getJdbcUrl());
        } catch (SQLException ex) {
            log.warn(
                    "pgvector 连接池预热失败（应用仍可启动，向量化功能不可用）: {}。请执行: docker compose -f docker/docker-compose.yml up -d pgvector",
                    ex.getMessage());
        }
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
