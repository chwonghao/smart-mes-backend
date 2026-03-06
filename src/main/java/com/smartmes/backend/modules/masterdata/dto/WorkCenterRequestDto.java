package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;

@Data
public class WorkCenterRequestDto {
    private String code;
    private String name;
    private String centerType; // e.g., "MACHINE", "ASSEMBLY_LINE"
    private Integer hourlyCapacity;
    private boolean isActive = true;
}