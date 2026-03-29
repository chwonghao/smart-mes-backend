package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.core.security.SecurityUtils;
import com.smartmes.backend.modules.masterdata.dto.WorkCenterRequestDto;
import com.smartmes.backend.modules.masterdata.service.WorkCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/work-centers")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "3. Master Data - Work Center", description = "APIs for managing machines, assembly lines, and workstations")
public class WorkCenterController {

    private final WorkCenterService service;

    @PostMapping
    @Operation(summary = "Create a new work center", description = "Register a new machine or assembly line into the system.")
    public ResponseEntity<?> createWorkCenter(@RequestBody WorkCenterRequestDto dto) {
        // GlobalExceptionHandler will catch any duplication errors automatically
        return ResponseEntity.ok(service.createWorkCenter(dto, SecurityUtils.getCurrentTenantId()));
    }

    @GetMapping
    @Operation(summary = "Get all work centers", description = "Retrieve a list of all work centers regardless of their active status.")
    public ResponseEntity<?> getAllWorkCenters() {
        return ResponseEntity.ok(service.getAllWorkCenters(SecurityUtils.getCurrentTenantId()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active work centers only", description = "Retrieve a list of work centers that are currently active and ready for production.")
    public ResponseEntity<?> getActiveWorkCenters() {
        return ResponseEntity.ok(service.getActiveWorkCenters(SecurityUtils.getCurrentTenantId()));
    }

    @PostMapping("/{id}/down")
    public ResponseEntity<?> reportMachineDown(
            @PathVariable Long id, 
            @RequestParam(name = "reason") String reason) { // Đảm bảo có @RequestParam
        service.reportMachineDown(id, reason, SecurityUtils.getCurrentTenantId());
        return ResponseEntity.ok("Machine reported as DOWN");
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve machine issue", description = "Marks the machine issue as fixed and calculates total downtime.")
    public ResponseEntity<?> resolveMachineIssue(@PathVariable Long id) {
        service.resolveMachineIssue(id, SecurityUtils.getCurrentTenantId());
        return ResponseEntity.ok("Machine issue resolved");
    }

    @PatchMapping("/{id}/ping")
    @Operation(summary = "Heartbeat Ping", description = "IoT devices call this every 60s to keep their status online.")
    public ResponseEntity<?> pingHeartbeat(@PathVariable Long id) {
        service.updatePing(id);
        return ResponseEntity.ok().build();
    }
}