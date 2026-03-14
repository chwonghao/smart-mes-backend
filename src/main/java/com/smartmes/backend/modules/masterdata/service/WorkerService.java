package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.WorkerDto;
import com.smartmes.backend.modules.masterdata.entity.Worker;
import com.smartmes.backend.modules.masterdata.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;

    public List<WorkerDto> getAllWorkers(String tenantId) {
        return workerRepository.findByTenantIdOrderByIdDesc(tenantId).stream().map(worker -> {
            WorkerDto dto = new WorkerDto();
            dto.setId(worker.getId());
            dto.setWorkerCode(worker.getWorkerCode());
            dto.setFullName(worker.getFullName());
            dto.setRole(worker.getRole());
            dto.setShift(worker.getShift());
            dto.setStatus(worker.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public WorkerDto createWorker(WorkerDto dto, String tenantId) {
        if (workerRepository.existsByWorkerCodeAndTenantId(dto.getWorkerCode(), tenantId)) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại!");
        }

        Worker worker = new Worker();
        worker.setWorkerCode(dto.getWorkerCode());
        worker.setFullName(dto.getFullName());
        worker.setRole(dto.getRole());
        worker.setShift(dto.getShift());
        worker.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        worker.setTenantId(tenantId);

        Worker saved = workerRepository.save(worker);
        dto.setId(saved.getId());
        return dto;
    }
}