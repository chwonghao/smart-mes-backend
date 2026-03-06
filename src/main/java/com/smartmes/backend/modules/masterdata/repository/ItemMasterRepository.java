package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemMasterRepository extends JpaRepository<ItemMaster, Long> {
    // Spring Data JPA ma thuật: Tự động dịch tên hàm thành câu lệnh SQL!
    boolean existsByItemCodeAndTenantId(String itemCode, String tenantId);
    
    List<ItemMaster> findByTenantIdAndIsDeletedFalse(String tenantId);
}