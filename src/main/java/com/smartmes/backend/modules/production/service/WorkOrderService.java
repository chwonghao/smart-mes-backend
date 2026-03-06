package com.smartmes.backend.modules.production.service;

import com.smartmes.backend.modules.inventory.service.InventoryService; // Thêm mới
import com.smartmes.backend.modules.masterdata.entity.Bom; // Thêm mới
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.repository.BomRepository; // Thêm mới
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.production.dto.ProductionProgressDto;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.dto.WorkOrderResponseDto;
import com.smartmes.backend.modules.production.entity.ProductionLog;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.ProductionLogRepository;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Thêm mới
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
    private final BomRepository bomRepository; // Thêm mới để truy vấn công thức
    private final InventoryService inventoryService; // Thêm mới để điều chỉnh kho
    private final ProductionLogRepository productionLogRepository;

    @Transactional
    public WorkOrderResponseDto createWorkOrder(WorkOrderRequestDto dto, String tenantId) {
        ItemMaster item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        String orderNumber = generateOrderNumber();

        List<Routing> routings = routingRepository
                .findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepNumberAsc(item.getId(), tenantId);
        int totalLeadTimeMinutes = routings.stream()
                .mapToInt(r -> r.getStandardTime() * dto.getPlannedQuantity())
                .sum();

        LocalDateTime plannedEndDate = dto.getPlannedStartDate().plusMinutes(totalLeadTimeMinutes);

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
        int randomPart = new Random().nextInt(900) + 100;
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

    @Transactional
    public WorkOrderResponseDto updateProgress(Long id, ProductionProgressDto dto, String tenantId) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        if (wo.getStatus() == WorkOrder.WorkOrderStatus.COMPLETED ||
                wo.getStatus() == WorkOrder.WorkOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update progress for a finished or cancelled order.");
        }

        int newActualQuantity = wo.getActualQuantity() + dto.getCompletedQuantity();

        if (newActualQuantity > wo.getPlannedQuantity()) {
            throw new IllegalArgumentException("Total completed quantity cannot exceed planned quantity!");
        }

        ProductionLog log = new ProductionLog();
        log.setWorkOrder(wo);
        log.setQuantityDone(dto.getCompletedQuantity());
        log.setNotes(dto.getNotes());
        log.setOperatorName("WORKER_01"); // Tạm thời hardcode
        log.setTenantId(tenantId);
        productionLogRepository.save(log);

        wo.setActualQuantity(newActualQuantity);

        // Cập nhật trạng thái
        if (newActualQuantity == 0) {
            wo.setStatus(WorkOrder.WorkOrderStatus.RELEASED);
        } else if (newActualQuantity < wo.getPlannedQuantity()) {
            wo.setStatus(WorkOrder.WorkOrderStatus.IN_PROGRESS);
        } else {
            // KHI LỆNH SẢN XUẤT HOÀN THÀNH
            wo.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
            processInventoryPostCompletion(wo, tenantId);
        }

        WorkOrder updated = workOrderRepository.save(wo);
        return mapToResponseDto(updated);
    }

    /**
     * Xử lý tự động Nhập kho Thành phẩm và Xuất kho Nguyên liệu dựa trên BOM
     */
    private void processInventoryPostCompletion(WorkOrder wo, String tenantId) {
        // 1. Cộng kho Thành phẩm (Finished Good)
        inventoryService.adjustStock(
                wo.getItem().getId(),
                new BigDecimal(wo.getPlannedQuantity()),
                tenantId);

        // 2. Trừ kho Nguyên vật liệu (Raw Materials) dựa trên định mức BOM
        List<Bom> bomList = bomRepository.findByParentItemIdAndTenantIdAndIsDeletedFalse(
                wo.getItem().getId(),
                tenantId);

        for (Bom bom : bomList) {
            // Lượng tiêu hao = Định mức * Số lượng sản xuất * (1 + Tỷ lệ hao hụt)
            BigDecimal consumptionQty = bom.getQuantity()
                    .multiply(new BigDecimal(wo.getPlannedQuantity()))
                    .multiply(BigDecimal.ONE.add(bom.getScrapFactor()));

            // Xuất kho (truyền số âm)
            inventoryService.adjustStock(
                    bom.getChildItem().getId(),
                    consumptionQty.negate(),
                    tenantId);
        }
    }
}