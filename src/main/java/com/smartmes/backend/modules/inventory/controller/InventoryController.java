package com.smartmes.backend.modules.inventory.controller;

import com.smartmes.backend.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "6. Inventory Management", description = "APIs for stock tracking and adjustments")
public class InventoryController {

    private final InventoryService service;
    private final String CURRENT_TENANT_ID = "TENANT_01";

    @GetMapping
    @Operation(summary = "Get all stock levels", description = "Retrieve current on-hand quantities for all items.")
    public ResponseEntity<?> getAllInventory() {
        return ResponseEntity.ok(service.getAllInventory(CURRENT_TENANT_ID));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Manual stock adjustment", description = "Increase or decrease stock manually (e.g., initial stock entry).")
    public ResponseEntity<?> adjustStock(@RequestParam Long itemId, @RequestParam BigDecimal amount) {
        service.adjustStock(itemId, amount, CURRENT_TENANT_ID);
        return ResponseEntity.ok("Stock adjusted successfully");
    }
}