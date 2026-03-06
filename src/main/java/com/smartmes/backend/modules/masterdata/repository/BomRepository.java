package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.Bom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long> {

    // 1. Get the complete recipe (all child items) for a specific parent product
    List<Bom> findByParentItemIdAndTenantIdAndIsDeletedFalse(Long parentItemId, String tenantId);

    // 2. Prevent duplication: Check if a material is already in the recipe
    boolean existsByParentItemIdAndChildItemIdAndTenantIdAndIsDeletedFalse(Long parentItemId, Long childItemId, String tenantId);
}