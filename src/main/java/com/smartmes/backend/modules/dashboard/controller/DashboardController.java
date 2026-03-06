package com.smartmes.backend.modules.dashboard.controller;

import com.smartmes.backend.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "0. Dashboard", description = "Real-time manufacturing KPIs and statistics")
public class DashboardController {

    private final DashboardService service;
    private final String CURRENT_TENANT_ID = "TENANT_01";

    @GetMapping("/stats")
    @Operation(summary = "Get overview statistics", description = "Retrieve a summary of work orders, production rates, and inventory levels.")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(service.getQuickStats(CURRENT_TENANT_ID));
    }
}