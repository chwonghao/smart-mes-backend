package com.smartmes.backend.modules.inventory.repository;

import com.smartmes.backend.modules.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // Find stock by Item ID and Tenant
    Optional<Inventory> findByItemIdAndTenantIdAndIsDeletedFalse(Long itemId, String tenantId);
}