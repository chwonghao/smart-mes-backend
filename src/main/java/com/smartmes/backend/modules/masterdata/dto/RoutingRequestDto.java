package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;

@Data
public class RoutingRequestDto {
    private Long itemId;
    private Integer stepNumber;
    private String operationName;
    private Long workCenterId;
    private Integer standardTime;
    private String description;
}