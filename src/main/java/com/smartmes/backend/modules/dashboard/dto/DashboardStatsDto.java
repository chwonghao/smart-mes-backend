package com.smartmes.backend.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatsDto {
    private long totalWorkOrders;
    private long activeWorkOrders; // IN_PROGRESS
    private long completedWorkOrders;
    private Map<String, Double> inventorySummary; // Tên vật tư -> Số lượng tồn
    private double overallCompletionRate; // Tỷ lệ hoàn thành kế hoạch tổng thể
}