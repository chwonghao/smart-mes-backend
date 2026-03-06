package com.smartmes.backend.modules.inventory.service;

import com.smartmes.backend.modules.inventory.entity.Inventory;
import com.smartmes.backend.modules.inventory.repository.InventoryRepository;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
                    newInv.setItem(itemRepository.findById(itemId).orElseThrow());
                    newInv.setTenantId(tenantId);
                    return newInv;
                });

        BigDecimal newQty = inv.getOnHandQuantity().add(amount);
        
        // Prevent negative stock
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient stock for item ID: " + itemId);
        }

        inv.setOnHandQuantity(newQty);
        inventoryRepository.save(inv);
    }
}