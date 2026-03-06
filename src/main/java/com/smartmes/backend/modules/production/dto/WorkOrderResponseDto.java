package com.smartmes.backend.modules.production.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderResponseDto {
    private Long id;
    private String orderNumber;
    private String itemName;
    private Integer plannedQuantity;
    private String status;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
}