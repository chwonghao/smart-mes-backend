package com.smartmes.backend.modules.masterdata.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class BomResponseDto {
    private Long id;
    private Long parentItemId;
    private String parentItemCode;
    private Long childItemId;
    private String childItemCode;
    private String childItemName;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal scrapFactor;
}