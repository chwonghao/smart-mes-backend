package com.smartmes.backend.modules.masterdata.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoutingResponseDto {
    private Long id;
    private Integer stepNumber;
    private String operationName;
    private String workCenterName;
    private Integer standardTime;
    private String description;
}