package com.smartmes.backend.modules.system;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    Page<SystemLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @Query("""
            SELECT l
            FROM SystemLog l
            WHERE l.tenantId = :tenantId
              AND (:module IS NULL OR l.module = :module)
              AND (:fromTime IS NULL OR l.createdAt >= :fromTime)
              AND (:toTime IS NULL OR l.createdAt <= :toTime)
            ORDER BY l.createdAt DESC
            """)
    Page<SystemLog> searchLogs(@Param("tenantId") String tenantId,
                               @Param("module") String module,
                               @Param("fromTime") LocalDateTime fromTime,
                               @Param("toTime") LocalDateTime toTime,
                               Pageable pageable);
}
