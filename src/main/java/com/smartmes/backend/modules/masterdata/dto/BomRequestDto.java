package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BomRequestDto {
    // ID of the finished/semi-finished product
    private Long parentItemId;
    
    // ID of the raw material to be consumed
    private Long childItemId;
    
    // Required quantity
    private BigDecimal quantity;
    
    // Allowed waste (e.g., 0.05 for 5%)
    private BigDecimal scrapFactor;
}