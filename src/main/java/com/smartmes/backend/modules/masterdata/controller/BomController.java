package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.modules.masterdata.dto.BomRequestDto;
import com.smartmes.backend.modules.masterdata.service.BomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/boms")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Tag(name = "2. Master Data - BOM (Bill of Materials)", description = "APIs for managing product recipes and material formulas")
public class BomController {

    private final BomService bomService;
    
    // Hardcoded for testing. Will be replaced by JWT context later.
    private final String CURRENT_TENANT_ID = "TENANT_01"; 

    @PostMapping
    @Operation(summary = "Add a material to BOM", description = "Link a child item (raw material) to a parent item (finished good).")
    public ResponseEntity<?> addMaterialToBom(@RequestBody BomRequestDto dto) {
        return ResponseEntity.ok(bomService.addMaterialToBom(dto, CURRENT_TENANT_ID));
    }

    @GetMapping("/{parentItemId}")
    @Operation(summary = "Get BOM tree by Parent Item", description = "Retrieve all child materials required to build the specified parent item.")
    public ResponseEntity<?> getBomTree(@PathVariable Long parentItemId) {
        return ResponseEntity.ok(bomService.getBomTree(parentItemId, CURRENT_TENANT_ID));
    }
}