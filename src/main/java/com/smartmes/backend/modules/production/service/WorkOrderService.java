package com.smartmes.backend.modules.production.service;

import com.smartmes.backend.modules.inventory.service.InventoryService;
import com.smartmes.backend.modules.masterdata.entity.Bom;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.repository.BomRepository;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.production.dto.ProductionProgressDto;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.dto.WorkOrderResponseDto;
import com.smartmes.backend.modules.production.entity.ProductionLog;
import com.smartmes.backend.modules.production.entity.QualityCheck; // Thêm mới
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.ProductionLogRepository;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import com.smartmes.backend.modules.realtime.dto.AlertNotificationDto;
import com.smartmes.backend.modules.realtime.service.AlertService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
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
    private final BomRepository bomRepository;
    private final InventoryService inventoryService;
    private final ProductionLogRepository productionLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;

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

        // Validate dữ liệu từ QC
        int passed = dto.getPassedQuantity() != null ? dto.getPassedQuantity() : dto.getCompletedQuantity();
        int failed = dto.getFailedQuantity() != null ? dto.getFailedQuantity() : 0;
        
        if (passed + failed != dto.getCompletedQuantity()) {
            throw new IllegalArgumentException("Passed and Failed quantities must equal Completed quantity.");
        }

        int newActualQuantity = wo.getActualQuantity() + dto.getCompletedQuantity();

        if (newActualQuantity > wo.getPlannedQuantity()) {
            throw new IllegalArgumentException("Total completed quantity cannot exceed planned quantity!");
        }

        // 1. Khởi tạo Production Log
        ProductionLog log = new ProductionLog();
        log.setWorkOrder(wo);
        log.setQuantityDone(dto.getCompletedQuantity());
        log.setNotes(dto.getNotes());
        log.setOperatorName("WORKER_01"); 
        log.setTenantId(tenantId);

        // 2. Khởi tạo Quality Check và liên kết với Log
        QualityCheck qc = new QualityCheck();
        qc.setProductionLog(log);
        qc.setPassedQuantity(passed);
        qc.setFailedQuantity(failed);
        qc.setDefectReason(dto.getDefectReason());
        qc.setInspectorName("QC_01");

        // Gắn ngược QC vào Log để tính năng Cascade hoạt động
        log.setQualityCheck(qc);

        productionLogRepository.save(log);
        // NẾU CÓ HÀNG LỖI -> BẮN THÔNG BÁO REAL-TIME NGAY LẬP TỨC
        if (failed > 0) {
            String alertMsg = String.format("CẢNH BÁO: Lệnh %s vừa phát sinh %d sản phẩm lỗi. Lý do: %s", 
                    wo.getOrderNumber(), failed, qc.getDefectReason());
            
            alertService.createAndSendAlert("QC_ALERT", alertMsg, tenantId);
        }

        wo.setActualQuantity(newActualQuantity);

        // 3. Cập nhật trạng thái
        if (newActualQuantity == 0) {
            wo.setStatus(WorkOrder.WorkOrderStatus.RELEASED);
        } else if (newActualQuantity < wo.getPlannedQuantity()) {
            wo.setStatus(WorkOrder.WorkOrderStatus.IN_PROGRESS);
        } else {
            wo.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
            processInventoryPostCompletion(wo, tenantId);
        }

        WorkOrder updated = workOrderRepository.save(wo);
        return mapToResponseDto(updated);
    }

    /**
     * Xử lý tự động Nhập kho Thành phẩm (hàng đạt) và Xuất kho Nguyên liệu (tổng tiêu hao)
     */
    private void processInventoryPostCompletion(WorkOrder wo, String tenantId) {
        // Tính tổng số lượng hàng đạt (Passed) từ tất cả các lần ghi nhận nhật ký
        List<ProductionLog> logs = productionLogRepository.findByWorkOrderIdOrderByCreatedAtDesc(wo.getId());
        int totalPassedQuantity = logs.stream()
                .mapToInt(log -> log.getQualityCheck() != null ? log.getQualityCheck().getPassedQuantity() : log.getQuantityDone())
                .sum();

        // 1. Cộng kho Thành phẩm (Chỉ cộng hàng Đạt chuẩn)
        inventoryService.adjustStock(
                wo.getItem().getId(),
                new BigDecimal(totalPassedQuantity),
                tenantId);

        // 2. Trừ kho Nguyên vật liệu (Dựa trên tổng khối lượng đã làm thực tế)
        List<Bom> bomList = bomRepository.findByParentItemIdAndTenantIdAndIsDeletedFalse(
                wo.getItem().getId(),
                tenantId);

        for (Bom bom : bomList) {
            // Dùng wo.getActualQuantity() vì nguyên liệu bị tiêu hao cho cả hàng Lỗi
            BigDecimal consumptionQty = bom.getQuantity()
                    .multiply(new BigDecimal(wo.getActualQuantity()))
                    .multiply(BigDecimal.ONE.add(bom.getScrapFactor()));

            inventoryService.adjustStock(
                    bom.getChildItem().getId(),
                    consumptionQty.negate(),
                    tenantId);
        }
    }
}