package com.smartmes.backend.modules.production.controller;

import com.smartmes.backend.core.security.SecurityUtils;
import com.smartmes.backend.modules.production.dto.ProductionProgressDto;
import com.smartmes.backend.modules.production.dto.ProductionScheduleDto;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.dto.WorkOrderResponseDto;
import com.smartmes.backend.modules.production.entity.ProductionSchedule;
import com.smartmes.backend.modules.production.repository.ProductionLogRepository;
import com.smartmes.backend.modules.production.service.ProductionScheduleService;
import com.smartmes.backend.modules.production.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/production/work-orders")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "5. Production - Work Order", description = "APIs for managing manufacturing orders and schedules")
public class WorkOrderController {

    private final WorkOrderService service;
    private final ProductionScheduleService productionScheduleService;
    private final ProductionLogRepository productionLogRepository;


    @PostMapping
    @Operation(summary = "Create a new Work Order", description = "Automatically generates order number and calculates end date based on routing.")
    public ResponseEntity<?> createWorkOrder(@RequestBody WorkOrderRequestDto dto) {
        return ResponseEntity.ok(service.createWorkOrder(dto, SecurityUtils.getCurrentTenantId()));
    }

    @PatchMapping("/{id}/progress")
    @Operation(summary = "Update production progress", description = "Report finished quantities and automatically update order status.")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestBody ProductionProgressDto dto) {
        return ResponseEntity.ok(service.updateProgress(id, dto, SecurityUtils.getCurrentTenantId()));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get production logs", description = "Retrieve history of progress reports for a specific work order.")
    public ResponseEntity<?> getProductionLogs(@PathVariable Long id) {
        // return ResponseEntity.ok(productionLogRepository.findByWorkOrderIdOrderByCreatedAtDesc(id));
        return ResponseEntity.ok(service.getWorkOrderLogs(id));
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponseDto>> getAllWorkOrders() {
        List<WorkOrderResponseDto> orders = service.getAllWorkOrders(SecurityUtils.getCurrentTenantId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/schedules")
    @Operation(summary = "Get production schedules for a work order", description = "Retrieve all assigned machines and their production schedules for a specific work order.")
    public ResponseEntity<List<ProductionScheduleDto>> getProductionSchedules(@PathVariable Long id) {
        List<ProductionSchedule> schedules = productionScheduleService.getSchedulesForWorkOrder(id);
        List<ProductionScheduleDto> dtos = schedules.stream()
                .map(ProductionScheduleDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/schedules/{scheduleId}/status")
    @Operation(summary = "Update production schedule status", description = "Update the status of a specific production schedule (PENDING, IN_PROGRESS, COMPLETED, SKIPPED).")
    public ResponseEntity<?> updateScheduleStatus(@PathVariable Long id, @PathVariable Long scheduleId, @RequestParam String status) {
        try {
            ProductionSchedule.ScheduleStatus newStatus = ProductionSchedule.ScheduleStatus.valueOf(status);
            productionScheduleService.updateScheduleStatus(scheduleId, newStatus);
            return ResponseEntity.ok("Schedule status updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        }
    }
}