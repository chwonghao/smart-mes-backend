package com.smartmes.backend.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationRunner ensureSoftDeleteColumnRunner() {
        return args -> {
            List<String> baseEntityTables = List.of(
                    "work_center",
                    "work_centers",
                    "work_order",
                    "work_orders",
                    "worker",
                    "workers",
                    "quality_check",
                    "quality_checks",
                    "production_log",
                    "production_logs",
                    "routing",
                    "routings",
                    "machine_downtime",
                    "machine_downtimes",
                    "bom",
                    "boms",
                    "item_master",
                    "item_masters",
                    "system_alert",
                    "system_alerts",
                    "inventory"
            );

            for (String table : baseEntityTables) {
                try {
                    jdbcTemplate.execute("ALTER TABLE IF EXISTS " + table + " ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE");
                } catch (Exception ex) {
                    log.warn("Skip ensuring is_deleted for table {} due to: {}", table, ex.getMessage());
                }
            }

            log.info("Schema compatibility check completed for soft-delete column (is_deleted)");
        };
    }
}
