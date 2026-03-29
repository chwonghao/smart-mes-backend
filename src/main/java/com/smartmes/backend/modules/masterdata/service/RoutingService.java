package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.RoutingDTO;
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

import java.util.ArrayList;
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
        ItemMaster item = itemRepository.findByIdAndTenantIdAndIsDeletedFalse(dto.getItemId(), tenantId)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + dto.getItemId()));

        // 2. Validate Work Center existence
        WorkCenter workCenter = workCenterRepository.findByIdAndTenantIdAndIsDeletedFalse(dto.getWorkCenterId(), tenantId)
                .orElseThrow(() -> new RuntimeException("Work Center not found with ID: " + dto.getWorkCenterId()));

        Integer effectiveSequence = dto.getStepSequence() != null ? dto.getStepSequence() : dto.getStepNumber();
        if (effectiveSequence == null || effectiveSequence < 1) {
            throw new IllegalArgumentException("stepSequence must be greater than 0");
        }

        // 3. Map to Entity
        Routing routing = new Routing();
        routing.setItem(item);
        routing.setStepNumber(effectiveSequence);
        routing.setStepSequence(effectiveSequence);
        routing.setOperationName(dto.getOperationName());
        routing.setWorkCenter(workCenter);
        routing.setStandardTime(dto.getStandardTime());
        routing.setDescription(dto.getDescription());
        routing.setTenantId(tenantId);
        routing.setCreatedBy("ADMIN");

        Routing saved = routingRepository.save(routing);
        return mapToResponseDto(saved);
    }

    @Transactional
    public List<RoutingResponseDto> syncRoutingSteps(Long itemId, List<RoutingDTO> steps, String tenantId) {
        ItemMaster item = itemRepository.findByIdAndTenantIdAndIsDeletedFalse(itemId, tenantId)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemId));

        // Soft-delete current routing steps for this item + tenant.
        List<Routing> existingSteps = routingRepository.findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepSequenceAscIdAsc(itemId, tenantId);
        if (!existingSteps.isEmpty()) {
            existingSteps.forEach(step -> {
                step.setDeleted(true);
                step.setUpdatedBy("SYSTEM");
            });
            routingRepository.saveAll(existingSteps);
        }

        if (steps == null || steps.isEmpty()) {
            return List.of();
        }

        List<Routing> newRoutes = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            RoutingDTO step = steps.get(i);
            Integer stepSequence = step.getStepSequence() != null ? step.getStepSequence() : (i + 1);
            if (stepSequence < 1) {
                throw new IllegalArgumentException("stepSequence must be greater than 0");
            }

            WorkCenter workCenter = workCenterRepository.findByIdAndTenantIdAndIsDeletedFalse(step.getWorkCenterId(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Work Center not found with ID: " + step.getWorkCenterId()));

            Routing routing = new Routing();
            routing.setItem(item);
            routing.setStepSequence(stepSequence);
            routing.setStepNumber(stepSequence); // keep backward compatibility with legacy field
            routing.setOperationName(step.getOperationName());
            routing.setWorkCenter(workCenter);
            routing.setStandardTime(step.getStandardTime());
            routing.setDescription(step.getDescription());
            routing.setTenantId(tenantId);
            routing.setCreatedBy("SYSTEM");

            newRoutes.add(routing);
        }

        return routingRepository.saveAll(newRoutes).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<RoutingResponseDto> getRoutingByItem(Long itemId, String tenantId) {
        return routingRepository.findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepSequenceAscIdAsc(itemId, tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private RoutingResponseDto mapToResponseDto(Routing routing) {
        return RoutingResponseDto.builder()
                .id(routing.getId())
                .stepNumber(routing.getStepNumber())
                .stepSequence(routing.getStepSequence())
                .operationName(routing.getOperationName())
                .workCenterName(routing.getWorkCenter().getName())
                .standardTime(routing.getStandardTime())
                .description(routing.getDescription())
                .build();
    }
}