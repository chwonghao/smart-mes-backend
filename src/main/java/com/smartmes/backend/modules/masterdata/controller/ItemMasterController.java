package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.modules.masterdata.dto.ItemMasterDto;
import com.smartmes.backend.modules.masterdata.service.ItemMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/items")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "1. Master Data - Item Master", description = "APIs for managing raw materials, semi-finished, and finished goods")
public class ItemMasterController {

    private final ItemMasterService service;
    
    // Hardcoded for testing. Will be replaced by JWT context later.
    private final String CURRENT_TENANT_ID = "TENANT_01"; 

    @PostMapping
    @Operation(summary = "Create a new item", description = "Save new item data into the system along with dynamic attributes (JSONB).")
    public ResponseEntity<?> createItem(@RequestBody ItemMasterDto dto) {
        return ResponseEntity.ok(service.createItem(dto, CURRENT_TENANT_ID));
    }

    @GetMapping
    @Operation(summary = "Get all items", description = "Retrieve the entire list of items for the currently logged-in tenant.")
    public ResponseEntity<?> getAllItems() {
        return ResponseEntity.ok(service.getAllItems(CURRENT_TENANT_ID));
    }
}