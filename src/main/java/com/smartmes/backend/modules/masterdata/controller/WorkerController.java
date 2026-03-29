package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.core.security.SecurityUtils;
import com.smartmes.backend.modules.masterdata.dto.WorkerDto;
import com.smartmes.backend.modules.masterdata.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/workers")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    public ResponseEntity<?> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers(SecurityUtils.getCurrentTenantId()));
    }

    @PostMapping
    public ResponseEntity<?> createWorker(@RequestBody WorkerDto dto) {
        try {
            return ResponseEntity.ok(workerService.createWorker(dto, SecurityUtils.getCurrentTenantId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}