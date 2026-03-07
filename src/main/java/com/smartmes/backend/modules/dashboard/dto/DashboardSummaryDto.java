package com.smartmes.backend.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryDto {
    private long totalMachines;
    private long downMachines;
    private long activeWorkOrders;
    private long completedWorkOrders;
    private double defectRate; // Tỷ lệ hàng lỗi (%)
}