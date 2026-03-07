package com.smartmes.backend.modules.masterdata.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkCenterResponseDto {
    private Long id;
    private String code;
    private String name;
    private String centerType;
    private Integer hourlyCapacity;
    private String currentStatus;
    private boolean isActive;
}