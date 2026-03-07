package com.smartmes.backend.modules.dashboard.service;

import com.smartmes.backend.modules.inventory.repository.InventoryRepository;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import com.smartmes.backend.modules.dashboard.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardStatsDto getQuickStats(String tenantId) {
        // 1. SỬA LỖI: Lọc chính xác theo tenantId
        List<WorkOrder> allOrders = workOrderRepository.findAllByTenantId(tenantId); 

        long total = allOrders.size();
        long active = allOrders.stream()
                .filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.IN_PROGRESS || o.getStatus() == WorkOrder.WorkOrderStatus.RELEASED)
                .count();
        long completed = allOrders.stream()
                .filter(o -> o.getStatus() == WorkOrder.WorkOrderStatus.COMPLETED)
                .count();

        // 2. SỬA LỖI: Tính tỷ lệ an toàn, chống chia cho 0 và giới hạn Max 100%
        double avgCompletion = allOrders.isEmpty() ? 0 : 
            allOrders.stream()
                .mapToDouble(o -> {
                    if (o.getPlannedQuantity() == null || o.getPlannedQuantity() == 0) return 0.0;
                    double rate = (double) o.getActualQuantity() / o.getPlannedQuantity() * 100;
                    return Math.min(rate, 100.0); // Không cho vượt quá 100% nếu làm dư
                })
                .average().orElse(0);

        // 3. HOÀN THIỆN: Lấy tồn kho, cộng dồn nếu trùng tên vật tư
        Map<String, Double> stockSummary = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getItem() != null)
                .collect(Collectors.toMap(
                    inv -> inv.getItem().getItemName(),
                    inv -> inv.getOnHandQuantity().doubleValue(),
                    Double::sum // CỘNG DỒN các lô hàng có cùng tên
                ));

        // 4. Lọc Top 5 vật tư có số lượng lớn nhất
        Map<String, Double> top5Stock = stockSummary.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                    Map.Entry::getKey, 
                    Map.Entry::getValue, 
                    (e1, e2) -> e1, 
                    java.util.LinkedHashMap::new // Giữ nguyên thứ tự sau khi sort
                ));

        return DashboardStatsDto.builder()
                .totalWorkOrders(total)
                .activeWorkOrders(active)
                .completedWorkOrders(completed)
                .overallCompletionRate(Math.round(avgCompletion * 100.0) / 100.0)
                .inventorySummary(top5Stock)
                .build();
    }
}