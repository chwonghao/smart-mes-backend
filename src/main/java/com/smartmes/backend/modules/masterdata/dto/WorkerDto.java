package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;

@Data
public class WorkerDto {
    private Long id;
    private String workerCode;
    private String fullName;
    private String role;
    private String shift;
    private String status;
}