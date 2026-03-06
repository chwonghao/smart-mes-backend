package com.smartmes.backend.modules.production.service;

import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.dto.WorkOrderResponseDto;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ItemMasterRepository itemRepository;
    private final RoutingRepository routingRepository;

    @Transactional
    public WorkOrderResponseDto createWorkOrder(WorkOrderRequestDto dto, String tenantId) {
        // 1. Validate Item
        ItemMaster item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // 2. Generate Work Order Number (WO-yyyyMMdd-XXXX)
        String orderNumber = generateOrderNumber();

        // 3. Calculate Estimated End Date based on Routing Standard Time
        List<Routing> routings = routingRepository.findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepNumberAsc(item.getId(), tenantId);
        int totalLeadTimeMinutes = routings.stream()
                .mapToInt(r -> r.getStandardTime() * dto.getPlannedQuantity())
                .sum();
        
        LocalDateTime plannedEndDate = dto.getPlannedStartDate().plusMinutes(totalLeadTimeMinutes);

        // 4. Map and Save Entity
        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNumber(orderNumber);
        workOrder.setItem(item);
        workOrder.setPlannedQuantity(dto.getPlannedQuantity());
        workOrder.setPlannedStartDate(dto.getPlannedStartDate());
        workOrder.setPlannedEndDate(plannedEndDate);
        workOrder.setPriority(dto.getPriority());
        workOrder.setTenantId(tenantId);
        workOrder.setCreatedBy("ADMIN");

        WorkOrder saved = workOrderRepository.save(workOrder);
        return mapToResponseDto(saved);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = new Random().nextInt(900) + 100; // Random 3 digits for demo
        return "WO-" + datePart + "-" + randomPart;
    }

    private WorkOrderResponseDto mapToResponseDto(WorkOrder wo) {
        return WorkOrderResponseDto.builder()
                .id(wo.getId())
                .orderNumber(wo.getOrderNumber())
                .itemName(wo.getItem().getItemName())
                .plannedQuantity(wo.getPlannedQuantity())
                .status(wo.getStatus().name())
                .plannedStartDate(wo.getPlannedStartDate())
                .plannedEndDate(wo.getPlannedEndDate())
                .build();
    }
}