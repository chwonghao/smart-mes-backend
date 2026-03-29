package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkCenterRepository extends JpaRepository<WorkCenter, Long> {
    
    // Prevent duplicate work center codes
    boolean existsByCodeAndTenantId(String code, String tenantId);
    
    // Fetch all available work centers for a specific tenant
    List<WorkCenter> findByTenantIdAndIsDeletedFalse(String tenantId);

    Optional<WorkCenter> findByIdAndTenantIdAndIsDeletedFalse(Long id, String tenantId);
    
    // Advanced feature: Fetch only ACTIVE machines for production scheduling
    List<WorkCenter> findByTenantIdAndIsActiveTrueAndIsDeletedFalse(String tenantId);
}