package com.smartmes.backend.modules.production.repository;

import com.smartmes.backend.modules.production.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    
    // Find work orders by status (useful for shop floor dashboard)
    List<WorkOrder> findByStatusAndTenantIdAndIsDeletedFalse(WorkOrder.WorkOrderStatus status, String tenantId);
    
    // Check if order number exists to prevent duplicates
    boolean existsByOrderNumberAndTenantId(String orderNumber, String tenantId);
}