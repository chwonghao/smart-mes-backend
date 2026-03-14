package com.smartmes.backend.modules.masterdata.controller;

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
    private final String CURRENT_TENANT = "TENANT_01"; // Tạm fix cứng cho đồ án

    @GetMapping
    public ResponseEntity<?> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers(CURRENT_TENANT));
    }

    @PostMapping
    public ResponseEntity<?> createWorker(@RequestBody WorkerDto dto) {
        try {
            return ResponseEntity.ok(workerService.createWorker(dto, CURRENT_TENANT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}