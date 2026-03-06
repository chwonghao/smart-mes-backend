package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.BomRequestDto;
import com.smartmes.backend.modules.masterdata.dto.BomResponseDto;
import com.smartmes.backend.modules.masterdata.entity.Bom;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.repository.BomRepository;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BomService {

    private final BomRepository bomRepository;
    private final ItemMasterRepository itemRepository;

    @Transactional
    public BomResponseDto addMaterialToBom(BomRequestDto dto, String tenantId) {
        // 1. Prevent circular reference (Parent cannot be its own child)
        if (dto.getParentItemId().equals(dto.getChildItemId())) {
            throw new IllegalArgumentException("Parent item and child item cannot be the same!");
        }

        // 2. Check if parent exists and is valid for production
        ItemMaster parent = itemRepository.findById(dto.getParentItemId())
                .orElseThrow(() -> new RuntimeException("Parent Item not found!"));
        
        if (parent.getItemType() == ItemMaster.ItemType.RAW_MATERIAL) {
            throw new IllegalArgumentException("Raw materials cannot have a BOM recipe!");
        }

        // 3. Check if child exists
        ItemMaster child = itemRepository.findById(dto.getChildItemId())
                .orElseThrow(() -> new RuntimeException("Child Item not found!"));

        // 4. Prevent duplicate components in the same recipe
        if (bomRepository.existsByParentItemIdAndChildItemIdAndTenantIdAndIsDeletedFalse(
                parent.getId(), child.getId(), tenantId)) {
            throw new IllegalArgumentException("This material already exists in the BOM!");
        }

        // 5. Create and map Entity
        Bom bom = new Bom();
        bom.setParentItem(parent);
        bom.setChildItem(child);
        bom.setQuantity(dto.getQuantity());
        bom.setScrapFactor(dto.getScrapFactor());
        bom.setUnit(child.getUnit());
        bom.setTenantId(tenantId);
        bom.setCreatedBy("ADMIN"); // Will be replaced by JWT later

        Bom savedBom = bomRepository.save(bom);

        // 6. Return mapped DTO
        return mapToResponseDto(savedBom);
    }

    public List<BomResponseDto> getBomTree(Long parentItemId, String tenantId) {
        List<Bom> boms = bomRepository.findByParentItemIdAndTenantIdAndIsDeletedFalse(parentItemId, tenantId);
        return boms.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private BomResponseDto mapToResponseDto(Bom bom) {
        return BomResponseDto.builder()
                .id(bom.getId())
                .parentItemId(bom.getParentItem().getId())
                .parentItemCode(bom.getParentItem().getItemCode())
                .childItemId(bom.getChildItem().getId())
                .childItemCode(bom.getChildItem().getItemCode())
                .childItemName(bom.getChildItem().getItemName())
                .quantity(bom.getQuantity())
                .unit(bom.getUnit())
                .scrapFactor(bom.getScrapFactor())
                .build();
    }
}