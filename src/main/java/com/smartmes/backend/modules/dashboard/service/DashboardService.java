package com.smartmes.backend.modules.dashboard.service;

import com.smartmes.backend.modules.inventory.repository.InventoryRepository;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import com.smartmes.backend.modules.dashboard.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardStatsDto getQuickStats(String tenantId) {
        List<WorkOrder> allOrders = workOrderRepository.findAll(); // Nên lọc theo tenantId

        long total = allOrders.size();
        long active = allOrders.stream().filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.IN_PROGRESS).count();
        long completed = allOrders.stream().filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.COMPLETED).count();

        // Tính tỷ lệ hoàn thành trung bình
        double avgCompletion = allOrders.isEmpty() ? 0 : 
            allOrders.stream()
                .mapToDouble(o -> (double) o.getActualQuantity() / o.getPlannedQuantity() * 100)
                .average().orElse(0);

        // Lấy tóm tắt tồn kho (Top 5 vật tư)
        var stockSummary = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                    inv -> inv.getItem().getItemName(),
                    inv -> inv.getOnHandQuantity().doubleValue(),
                    (existing, replacement) -> existing
                ));

        return DashboardStatsDto.builder()
                .totalWorkOrders(total)
                .activeWorkOrders(active)
                .completedWorkOrders(completed)
                .overallCompletionRate(Math.round(avgCompletion * 100.0) / 100.0)
                .inventorySummary(stockSummary)
                .build();
    }
}