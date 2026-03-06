package com.smartmes.backend.modules.production.controller;

import com.smartmes.backend.modules.production.dto.ProductionProgressDto;
import com.smartmes.backend.modules.production.dto.WorkOrderRequestDto;
import com.smartmes.backend.modules.production.repository.ProductionLogRepository;
import com.smartmes.backend.modules.production.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/production/work-orders")
@RequiredArgsConstructor
@Tag(name = "5. Production - Work Order", description = "APIs for managing manufacturing orders and schedules")
public class WorkOrderController {

    private final WorkOrderService service;
    private final String CURRENT_TENANT_ID = "TENANT_01";
    private final ProductionLogRepository productionLogRepository;


    @PostMapping
    @Operation(summary = "Create a new Work Order", description = "Automatically generates order number and calculates end date based on routing.")
    public ResponseEntity<?> createWorkOrder(@RequestBody WorkOrderRequestDto dto) {
        return ResponseEntity.ok(service.createWorkOrder(dto, CURRENT_TENANT_ID));
    }

    @PatchMapping("/{id}/progress")
    @Operation(summary = "Update production progress", description = "Report finished quantities and automatically update order status.")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestBody ProductionProgressDto dto) {
        return ResponseEntity.ok(service.updateProgress(id, dto, CURRENT_TENANT_ID));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get production logs", description = "Retrieve history of progress reports for a specific work order.")
    public ResponseEntity<?> getProductionLogs(@PathVariable Long id) {
        return ResponseEntity.ok(productionLogRepository.findByWorkOrderIdOrderByCreatedAtDesc(id));
    }
}