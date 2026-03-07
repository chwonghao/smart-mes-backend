package com.smartmes.backend.modules.production.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkOrderRequestDto {
    private Long itemId;
    private Integer plannedQuantity;
    private LocalDateTime plannedStartDate;
    private Integer priority; // 1: Low, 2: Medium, 3: High
    private Long workCenterId;
}