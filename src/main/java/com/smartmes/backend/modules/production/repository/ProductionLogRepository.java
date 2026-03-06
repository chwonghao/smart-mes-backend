package com.smartmes.backend.modules.production.repository;

import com.smartmes.backend.modules.production.entity.ProductionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {
    List<ProductionLog> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
}