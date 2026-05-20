package com.aios.platform.kb.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 启动时校验 pgvector 表是否可用，避免误连 MySQL 导致向量化 SQL 报错。 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aios.vector.enabled", havingValue = "true", matchIfMissing = true)
public class VectorStoreSchemaValidator {

    private final JdbcTemplate vectorJdbc;
    private final DataSource vectorDataSource;

    public VectorStoreSchemaValidator(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbc,
            @Qualifier("vectorDataSource") DataSource vectorDataSource) {
        this.vectorJdbc = vectorJdbc;
        this.vectorDataSource = vectorDataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (vectorDataSource instanceof HikariDataSource hikari) {
            log.info("pgvector 数据源: {}", hikari.getJdbcUrl());
            if (!hikari.getJdbcUrl().contains("postgresql")) {
                log.error("向量库 JDBC URL 不是 PostgreSQL，向量化将失败。请检查 aios.vector.datasource.url");
            }
        }
        try {
            vectorJdbc.queryForObject("SELECT COUNT(*) FROM kb_chunk_vector", Long.class);
            log.info("pgvector 表 kb_chunk_vector 校验通过");
        } catch (Exception ex) {
            log.error(
                    "pgvector 表 kb_chunk_vector 不可用: {}。请执行: ./scripts/db-init-pgvector.sh",
                    ex.getMessage());
        }
    }
}
