package com.smartmes.backend.modules.inventory.service;

import com.smartmes.backend.modules.inventory.dto.InventoryResponseDto; // Import mới
import com.smartmes.backend.modules.inventory.entity.Inventory;
import com.smartmes.backend.modules.inventory.repository.InventoryRepository;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List; // Import mới
import java.util.stream.Collectors; // Import mới

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ItemMasterRepository itemRepository;

    @Transactional
    public void adjustStock(Long itemId, BigDecimal amount, String tenantId) {
        Inventory inv = inventoryRepository.findByItemIdAndTenantIdAndIsDeletedFalse(itemId, tenantId)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setItem(itemRepository.findById(itemId)
                            .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemId)));
                    newInv.setTenantId(tenantId);
                    newInv.setOnHandQuantity(BigDecimal.ZERO); // Đảm bảo khởi tạo là 0
                    return newInv;
                });

        BigDecimal newQty = inv.getOnHandQuantity().add(amount);
        
        // Ngăn chặn kho âm
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient stock for item ID: " + itemId);
        }

        inv.setOnHandQuantity(newQty);
        inventoryRepository.save(inv);
    }

    /**
     * Lấy toàn bộ danh sách tồn kho của Tenant hiện tại
     */
    public List<InventoryResponseDto> getAllInventory(String tenantId) {
        // Giả sử bạn có method findByTenantIdAndIsDeletedFalse trong Repository
        // Nếu chưa có, bạn có thể dùng findAll() tạm thời để test
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getTenantId().equals(tenantId) && !inv.isDeleted())
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Ánh xạ từ Entity sang DTO để trả về cho Client
     */
    private InventoryResponseDto mapToResponseDto(Inventory inv) {
        return InventoryResponseDto.builder()
                .itemId(inv.getItem().getId())
                .itemCode(inv.getItem().getItemCode())
                .itemName(inv.getItem().getItemName())
                .onHandQuantity(inv.getOnHandQuantity())
                .unit(inv.getItem().getUnit())
                .locationCode(inv.getLocationCode())
                .build();
    }
}