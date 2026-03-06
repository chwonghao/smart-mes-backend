package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.WorkCenterRequestDto;
import com.smartmes.backend.modules.masterdata.dto.WorkCenterResponseDto;
import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkCenterService {

    private final WorkCenterRepository repository;

    @Transactional
    public WorkCenterResponseDto createWorkCenter(WorkCenterRequestDto dto, String tenantId) {
        // 1. Prevent duplicate work center codes
        if (repository.existsByCodeAndTenantId(dto.getCode(), tenantId)) {
            throw new IllegalArgumentException("Work Center code '" + dto.getCode() + "' already exists!");
        }

        // 2. Map DTO to Entity
        WorkCenter workCenter = new WorkCenter();
        workCenter.setCode(dto.getCode());
        workCenter.setName(dto.getName());
        workCenter.setCenterType(WorkCenter.CenterType.valueOf(dto.getCenterType()));
        workCenter.setHourlyCapacity(dto.getHourlyCapacity());
        workCenter.setActive(dto.isActive());
        
        workCenter.setTenantId(tenantId);
        workCenter.setCreatedBy("ADMIN");

        // 3. Save to database
        WorkCenter savedWorkCenter = repository.save(workCenter);

        // 4. Return mapped Response DTO
        return mapToResponseDto(savedWorkCenter);
    }

    // Get all work centers (including inactive ones for management)
    public List<WorkCenterResponseDto> getAllWorkCenters(String tenantId) {
        return repository.findByTenantIdAndIsDeletedFalse(tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // Get ONLY active work centers (for Production Scheduling dropdowns)
    public List<WorkCenterResponseDto> getActiveWorkCenters(String tenantId) {
        return repository.findByTenantIdAndIsActiveTrueAndIsDeletedFalse(tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private WorkCenterResponseDto mapToResponseDto(WorkCenter workCenter) {
        return WorkCenterResponseDto.builder()
                .id(workCenter.getId())
                .code(workCenter.getCode())
                .name(workCenter.getName())
                .centerType(workCenter.getCenterType().name())
                .hourlyCapacity(workCenter.getHourlyCapacity())
                .isActive(workCenter.isActive())
                .build();
    }
}