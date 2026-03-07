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
    private Integer plannedQuantity;   // Số lượng mục tiêu
    private Integer actualQuantity;    // Số lượng đã làm (Passed + Failed)
    private String status;
    private Long workCenterId;         // ID máy đang chạy lệnh này
    private String workCenterName;     // Tên máy để hiển thị lên bảng
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
}