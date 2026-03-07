package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.modules.masterdata.dto.RoutingRequestDto;
import com.smartmes.backend.modules.masterdata.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/routings")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "4. Master Data - Routing", description = "APIs for defining production process steps (Operations)")
public class RoutingController {

    private final RoutingService service;
    private final String CURRENT_TENANT_ID = "TENANT_01";

    @PostMapping
    @Operation(summary = "Add a production step", description = "Define an operation step for an item at a specific work center.")
    public ResponseEntity<?> createRouting(@RequestBody RoutingRequestDto dto) {
        return ResponseEntity.ok(service.createRoutingStep(dto, CURRENT_TENANT_ID));
    }

    @GetMapping("/item/{itemId}")
    @Operation(summary = "Get routing by item", description = "Retrieve the sequence of production steps for a specific product.")
    public ResponseEntity<?> getRoutingByItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(service.getRoutingByItem(itemId, CURRENT_TENANT_ID));
    }
}