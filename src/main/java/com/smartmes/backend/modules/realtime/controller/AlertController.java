package com.smartmes.backend.modules.realtime.controller;

import com.smartmes.backend.modules.realtime.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/realtime/alerts")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "7. Realtime Alerts", description = "APIs for managing system notifications and QC alerts")
public class AlertController {

    private final AlertService service;
    private final String CURRENT_TENANT_ID = "TENANT_01";

    @GetMapping("/unread")
    @Operation(summary = "Get unread alerts", description = "Retrieve a list of all unread system alerts.")
    public ResponseEntity<?> getUnreadAlerts() {
        return ResponseEntity.ok(service.getUnreadAlerts(CURRENT_TENANT_ID));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark alert as read", description = "Acknowledge an alert so it disappears from the unread list.")
    public ResponseEntity<?> markAlertAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return ResponseEntity.ok("Alert marked as read");
    }

    @GetMapping
    public ResponseEntity<?> getAllAlerts() {
        String tenantId = "TENANT_01"; // Hoặc lấy từ Token
        return ResponseEntity.ok(service.getAllAlerts(tenantId));
    }
}