package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.Routing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingRepository extends JpaRepository<Routing, Long> {
    
    // Get all steps of a production process for an item, ordered by step number
    List<Routing> findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepNumberAsc(Long itemId, String tenantId);
}