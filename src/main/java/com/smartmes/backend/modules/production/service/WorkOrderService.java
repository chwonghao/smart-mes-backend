package com.smartmes.backend.modules.production.service;

import com.smartmes.backend.modules.inventory.service.InventoryService;
import com.smartmes.backend.modules.masterdata.entity.Bom;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.BomRepository;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import com.smartmes.backend.modules.production.dto.ProductionProgressDto;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.dto.WorkOrderResponseDto;
import com.smartmes.backend.modules.production.entity.ProductionLog;
import com.smartmes.backend.modules.production.entity.QualityCheck;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.ProductionLogRepository;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import com.smartmes.backend.modules.realtime.service.AlertService;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
// IMPORT THÊM ĐỂ XỬ LÝ DTO (MAP)
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ItemMasterRepository itemRepository;
    private final RoutingRepository routingRepository;
    private final BomRepository bomRepository;
    private final InventoryService inventoryService;
    private final ProductionLogRepository productionLogRepository;
    private final AlertService alertService;
    private final WorkCenterRepository workCenterRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

        if (dto.getWorkCenterId() != null) {
            WorkCenter wc = workCenterRepository.findById(dto.getWorkCenterId())
                    .orElseThrow(() -> new RuntimeException("Work Center not found"));
            workOrder.setWorkCenter(wc);
        }

        WorkOrder saved = workOrderRepository.save(workOrder);
        messagingTemplate.convertAndSend("/topic/dashboard", "NEW_ORDER");
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
                .actualQuantity(wo.getActualQuantity()) 
                .status(wo.getStatus().name())
                .workCenterId(wo.getWorkCenter() != null ? wo.getWorkCenter().getId() : null) 
                .workCenterName(wo.getWorkCenter() != null ? wo.getWorkCenter().getName() : "Chưa gán") 
                .plannedStartDate(wo.getPlannedStartDate())
                .plannedEndDate(wo.getPlannedEndDate())
                .build();
    }

    @Transactional
    public WorkOrderResponseDto updateProgress(Long id, ProductionProgressDto dto, String tenantId) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work Order not found"));

        if (dto.getWorkCenterId() == null) {
            throw new IllegalArgumentException("Bắt buộc phải chọn Máy/Trạm làm việc (workCenterId) để báo cáo!");
        }

        WorkCenter wc = workCenterRepository.findById(dto.getWorkCenterId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Máy/Trạm làm việc này"));

        if (wc.getCurrentStatus() == WorkCenter.MachineStatus.DOWN ||
                wc.getCurrentStatus() == WorkCenter.MachineStatus.OFFLINE) {
            throw new IllegalStateException(String.format(
                    "TỪ CHỐI GHI NHẬN: Máy [%s] hiện đang ở trạng thái %s. Không thể sản xuất!",
                    wc.getName(), wc.getCurrentStatus()));
        }

        if (wo.getStatus() == WorkOrder.WorkOrderStatus.COMPLETED ||
                wo.getStatus() == WorkOrder.WorkOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update progress for a finished or cancelled order.");
        }

        int passed = dto.getPassedQuantity() != null ? dto.getPassedQuantity() : dto.getCompletedQuantity();
        int failed = dto.getFailedQuantity() != null ? dto.getFailedQuantity() : 0;

        if (passed + failed != dto.getCompletedQuantity()) {
            throw new IllegalArgumentException("Passed and Failed quantities must equal Completed quantity.");
        }

        int newActualQuantity = wo.getActualQuantity() + dto.getCompletedQuantity();

        if (newActualQuantity > wo.getPlannedQuantity()) {
            throw new IllegalArgumentException("Total completed quantity cannot exceed planned quantity!");
        }

        processInventoryForProgress(wo, passed, dto.getCompletedQuantity(), tenantId);

        ProductionLog log = new ProductionLog();
        log.setWorkOrder(wo);
        log.setWorkCenter(wc);
        log.setQuantityDone(dto.getCompletedQuantity());
        log.setNotes(dto.getNotes());
        log.setOperatorName(dto.getOperatorName() != null ? dto.getOperatorName() : "Unknown Worker");
        log.setTenantId(tenantId);

        QualityCheck qc = new QualityCheck();
        qc.setProductionLog(log);
        qc.setPassedQuantity(passed);
        qc.setFailedQuantity(failed);
        qc.setDefectReason(dto.getDefectReason());
        qc.setInspectorName(dto.getOperatorName() != null ? dto.getOperatorName() : "Unknown QC");
        qc.setTenantId(tenantId);
        log.setQualityCheck(qc);

        productionLogRepository.save(log);

        if (failed > 0) {
            String alertMsg = String.format("CẢNH BÁO: Lệnh %s vừa phát sinh %d sản phẩm lỗi. Lý do: %s",
                    wo.getOrderNumber(), failed, qc.getDefectReason());
            alertService.createAndSendAlert("QC_ALERT", alertMsg, tenantId);
        }

        wo.setActualQuantity(newActualQuantity);

        if (newActualQuantity == 0) {
            wo.setStatus(WorkOrder.WorkOrderStatus.RELEASED);
        } else if (newActualQuantity < wo.getPlannedQuantity()) {
            wo.setStatus(WorkOrder.WorkOrderStatus.IN_PROGRESS);
            wc.setCurrentStatus(WorkCenter.MachineStatus.RUNNING); 
        } else {
            wo.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
            wc.setCurrentStatus(WorkCenter.MachineStatus.IDLE); 
        }

        WorkOrder updated = workOrderRepository.save(wo);
        messagingTemplate.convertAndSend("/topic/dashboard", "PROGRESS_UPDATED");
        return mapToResponseDto(updated);
    }

    private void processInventoryForProgress(WorkOrder wo, int passedQty, int totalCompletedQty, String tenantId) {
        if (passedQty > 0) {
            inventoryService.adjustStock(wo.getItem().getId(), new BigDecimal(passedQty), tenantId);
        }

        if (totalCompletedQty > 0) {
            List<Bom> bomList = bomRepository.findByParentItemIdAndTenantIdAndIsDeletedFalse(
                    wo.getItem().getId(), tenantId);

            for (Bom bom : bomList) {
                BigDecimal consumptionQty = bom.getQuantity()
                        .multiply(new BigDecimal(totalCompletedQty))
                        .multiply(BigDecimal.ONE.add(bom.getScrapFactor()));

                inventoryService.adjustStock(bom.getChildItem().getId(), consumptionQty.negate(), tenantId);
            }
        }
    }

    public List<WorkOrderResponseDto> getAllWorkOrders(String tenantId) {
        return workOrderRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // =========================================================================
    // THÊM HÀM MỚI: BÓC TÁCH DỮ LIỆU ĐỂ TRÁNH LỖI HIBERNATE LAZY PROXY (LỖI 500)
    // =========================================================================
    public List<Map<String, Object>> getWorkOrderLogs(Long workOrderId) {
        // Gọi repository để lấy logs
        List<ProductionLog> logs = productionLogRepository.findByWorkOrderId(workOrderId);

        // Map dữ liệu sang dạng phẳng (Map/DTO) để cắt đứt liên kết với Proxy
        return logs.stream().map(log -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", log.getId());
            dto.put("createdAt", log.getCreatedAt());
            dto.put("operatorName", log.getOperatorName());
            dto.put("quantityDone", log.getQuantityDone());
            dto.put("notes", log.getNotes());
            return dto;
        }).toList();
    }
}