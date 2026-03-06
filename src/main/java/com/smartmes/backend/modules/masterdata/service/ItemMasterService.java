package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.ItemMasterDto;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemMasterService {

    private final ItemMasterRepository repository;

    // Hàm tạo mới vật tư
    public ItemMaster createItem(ItemMasterDto dto, String tenantId) {
        // 1. Kiểm tra trùng mã vật tư trong cùng 1 công ty
        if (repository.existsByItemCodeAndTenantId(dto.getItemCode(), tenantId)) {
            throw new RuntimeException("Mã vật tư " + dto.getItemCode() + " đã tồn tại!");
        }

        // 2. Chuyển đổi từ DTO sang Entity để lưu Database
        ItemMaster entity = new ItemMaster();
        entity.setItemCode(dto.getItemCode());
        entity.setItemName(dto.getItemName());
        entity.setItemType(ItemMaster.ItemType.valueOf(dto.getItemType()));
        entity.setUnit(dto.getUnit());
        entity.setCustomAttributes(dto.getCustomAttributes());
        
        // Gán tenantId và người tạo (Tạm thời hardcode, sau này sẽ lấy từ JWT Token)
        entity.setTenantId(tenantId);
        entity.setCreatedBy("ADMIN");

        // 3. Lưu xuống Database
        return repository.save(entity);
    }

    // Hàm lấy danh sách vật tư
    public List<ItemMaster> getAllItems(String tenantId) {
        return repository.findByTenantIdAndIsDeletedFalse(tenantId);
    }
}