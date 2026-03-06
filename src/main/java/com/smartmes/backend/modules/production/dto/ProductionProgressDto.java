package com.smartmes.backend.modules.production.dto;

import lombok.Data;

@Data
public class ProductionProgressDto {
    // Number of items finished in this batch
    private Integer completedQuantity;
    
    // Optional notes from the operator
    private String notes;
}