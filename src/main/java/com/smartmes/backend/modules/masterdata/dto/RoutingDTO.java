package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;

@Data
public class RoutingDTO {
    private Integer stepSequence;
    private String operationName;
    private Long workCenterId;
    private Integer standardTime;
    private String description;
}
