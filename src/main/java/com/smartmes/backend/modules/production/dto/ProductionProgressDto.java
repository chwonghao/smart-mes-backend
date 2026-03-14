package com.smartmes.backend.modules.production.dto;

import lombok.Data;

@Data
public class ProductionProgressDto {
    private Long workCenterId;
    private Integer completedQuantity; // Tổng số lượng báo cáo lần này (Pass + NG)
    private Integer passedQuantity;    // Số lượng đạt chuẩn
    private Integer failedQuantity;    // Số lượng lỗi
    private String defectReason;       // Lý do lỗi (nếu có)
    private String operatorName;
    private String notes;
}