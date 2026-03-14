package com.smartmes.backend.modules.realtime.repository;

import com.smartmes.backend.modules.realtime.entity.SystemAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemAlertRepository extends JpaRepository<SystemAlert, Long> {
    // Lấy danh sách cảnh báo mới nhất chưa đọc
    List<SystemAlert> findByTenantIdAndIsReadFalseOrderByCreatedAtDesc(String tenantId);
    // Lấy toàn bộ thông báo
    List<SystemAlert> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}