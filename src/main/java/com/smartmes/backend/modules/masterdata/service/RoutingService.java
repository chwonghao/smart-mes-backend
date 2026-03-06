package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.RoutingRequestDto;
import com.smartmes.backend.modules.masterdata.dto.RoutingResponseDto;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RoutingRepository routingRepository;
    private final ItemMasterRepository itemRepository;
    private final WorkCenterRepository workCenterRepository;

    @Transactional
    public RoutingResponseDto createRoutingStep(RoutingRequestDto dto, String tenantId) {
        // 1. Validate Item existence
        ItemMaster item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + dto.getItemId()));

        // 2. Validate Work Center existence
        WorkCenter workCenter = workCenterRepository.findById(dto.getWorkCenterId())
                .orElseThrow(() -> new RuntimeException("Work Center not found with ID: " + dto.getWorkCenterId()));

        // 3. Map to Entity
        Routing routing = new Routing();
        routing.setItem(item);
        routing.setStepNumber(dto.getStepNumber());
        routing.setOperationName(dto.getOperationName());
        routing.setWorkCenter(workCenter);
        routing.setStandardTime(dto.getStandardTime());
        routing.setDescription(dto.getDescription());
        routing.setTenantId(tenantId);
        routing.setCreatedBy("ADMIN");

        Routing saved = routingRepository.save(routing);
        return mapToResponseDto(saved);
    }

    public List<RoutingResponseDto> getRoutingByItem(Long itemId, String tenantId) {
        return routingRepository.findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepNumberAsc(itemId, tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private RoutingResponseDto mapToResponseDto(Routing routing) {
        return RoutingResponseDto.builder()
                .id(routing.getId())
                .stepNumber(routing.getStepNumber())
                .operationName(routing.getOperationName())
                .workCenterName(routing.getWorkCenter().getName())
                .standardTime(routing.getStandardTime())
                .description(routing.getDescription())
                .build();
    }
}