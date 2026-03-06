package com.smartmes.backend.modules.inventory.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InventoryResponseDto {
    private Long itemId;
    private String itemCode;
    private String itemName;
    private BigDecimal onHandQuantity;
    private String unit;
    private String locationCode;
}