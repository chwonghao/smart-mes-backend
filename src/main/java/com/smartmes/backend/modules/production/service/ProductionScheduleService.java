package com.smartmes.backend.modules.production.service;

import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import com.smartmes.backend.modules.masterdata.entity.Routing;
import com.smartmes.backend.modules.masterdata.repository.ItemMasterRepository;
import com.smartmes.backend.modules.masterdata.repository.RoutingRepository;
import com.smartmes.backend.modules.production.dto.ProductionScheduleDto;
import com.smartmes.backend.modules.production.entity.ProductionSchedule;
import com.smartmes.backend.modules.production.entity.WorkOrder;
import com.smartmes.backend.modules.production.repository.ProductionScheduleRepository;
import com.smartmes.backend.modules.production.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing ProductionSchedule entities.
 * Handles automatic assignment of machines to work orders based on product routing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionScheduleService {

    private final ProductionScheduleRepository productionScheduleRepository;
    private final WorkOrderRepository workOrderRepository;
    private final RoutingRepository routingRepository;
    private final ItemMasterRepository itemMasterRepository;

    /**
     * Creates ProductionSchedule entries for a work order based on the item's routing steps.
     * This automatically assigns all machines from the product's routing process.
     *
     * @param workOrderId ID of the work order
     * @param itemId ID of the item/product
     * @param tenantId Tenant identifier for multi-tenancy
     * @return List of created ProductionSchedule entries
     */
    @Transactional
    public List<ProductionSchedule> createSchedulesFromRouting(Long workOrderId, Long itemId, String tenantId) {
        log.info("Creating production schedules for workOrder={}, item={}, tenant={}", workOrderId, itemId, tenantId);

        // Fetch the work order
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        // Fetch the item
        ItemMaster item = itemMasterRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        // Fetch routing steps for this item, ordered by step number
        List<Routing> routingSteps = routingRepository
                .findByItemIdAndTenantIdAndIsDeletedFalseOrderByStepNumberAsc(itemId, tenantId);

        if (routingSteps.isEmpty()) {
            log.warn("No routing steps found for item={}", itemId);
            return new ArrayList<>();
        }

        List<ProductionSchedule> schedules = new ArrayList<>();

        // Create ProductionSchedule entry for each routing step
        for (int index = 0; index < routingSteps.size(); index++) {
            Routing routing = routingSteps.get(index);
            ProductionSchedule schedule = new ProductionSchedule();

            schedule.setWorkOrder(workOrder);
            schedule.setWorkCenter(routing.getWorkCenter());
            schedule.setSequenceNumber(index + 1); // 1-based sequence
            schedule.setQuantityTarget(workOrder.getPlannedQuantity());
            schedule.setQuantityCompleted(0);
            schedule.setStatus(ProductionSchedule.ScheduleStatus.PENDING);
            schedule.setTenantId(tenantId);

            // Set planned dates based on work order dates and standard time
            if (workOrder.getPlannedStartDate() != null) {
                schedule.setEstimatedStartTime(workOrder.getPlannedStartDate().plusMinutes((long) index * 60));
                if (routing.getStandardTime() != null) {
                    schedule.setEstimatedEndTime(schedule.getEstimatedStartTime().plusMinutes(routing.getStandardTime()));
                }
            }

            schedules.add(schedule);
            log.debug("Created schedule: workCenter={}, sequence={}", routing.getWorkCenter().getId(), index + 1);
        }

        // Save all schedules
        List<ProductionSchedule> savedSchedules = productionScheduleRepository.saveAll(schedules);
        log.info("Successfully created {} production schedules for workOrder={}", savedSchedules.size(), workOrderId);

        return savedSchedules;
    }

    /**
     * Fetches all production schedules for a given work order.
     *
     * @param workOrderId ID of the work order
     * @return List of ProductionSchedule entries ordered by sequence
     */
    public List<ProductionSchedule> getSchedulesForWorkOrder(Long workOrderId) {
        return productionScheduleRepository.findByWorkOrderIdOrderBySequenceNumber(workOrderId);
    }

    /**
     * Returns schedule DTOs for a work order, creating fallback data when the order
     * has not been migrated to production schedules yet.
     *
     * @param workOrderId ID of the work order
     * @param tenantId Tenant identifier for multi-tenancy
     * @return List of schedule DTOs
     */
    @Transactional(readOnly = true)
    public List<ProductionScheduleDto> getScheduleDtosForWorkOrder(Long workOrderId, String tenantId) {
        List<ProductionSchedule> schedules = getSchedulesForWorkOrder(workOrderId);
        if (!schedules.isEmpty()) {
            return schedules.stream()
                    .map(ProductionScheduleDto::fromEntity)
                    .toList();
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId).orElse(null);
        if (workOrder == null) {
            log.warn("Work order {} not found when resolving schedules", workOrderId);
            return List.of();
        }

        if (workOrder.getItem() != null) {
            List<ProductionSchedule> generatedSchedules = createSchedulesFromRouting(workOrderId, workOrder.getItem().getId(), tenantId);
            if (!generatedSchedules.isEmpty()) {
                return generatedSchedules.stream()
                        .map(ProductionScheduleDto::fromEntity)
                        .toList();
            }
        }

        if (workOrder.getWorkCenter() != null) {
            return List.of(ProductionScheduleDto.builder()
                    .id(null)
                    .workOrderId(workOrder.getId())
                    .workCenterId(workOrder.getWorkCenter().getId())
                    .workCenterName(workOrder.getWorkCenter().getName())
                    .sequenceNumber(1)
                    .quantityTarget(workOrder.getPlannedQuantity())
                    .quantityCompleted(workOrder.getActualQuantity() != null ? workOrder.getActualQuantity() : 0)
                    .status(workOrder.getStatus() != null ? workOrder.getStatus().name() : "PENDING")
                    .estimatedStartTime(workOrder.getPlannedStartDate())
                    .estimatedEndTime(workOrder.getPlannedEndDate())
                    .build());
        }

        return List.of();
    }

    /**
     * Fetches all active schedules for a specific work center.
     *
     * @param workCenterId ID of the work center
     * @return List of ProductionSchedule entries assigned to this work center
     */
    public List<ProductionSchedule> getSchedulesForWorkCenter(Long workCenterId) {
        return productionScheduleRepository.findByWorkCenterId(workCenterId);
    }

    /**
     * Updates the status of a production schedule.
     *
     * @param scheduleId ID of the production schedule
     * @param status New status
     */
    @Transactional
    public void updateScheduleStatus(Long scheduleId, ProductionSchedule.ScheduleStatus status) {
        ProductionSchedule schedule = productionScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Production schedule not found: " + scheduleId));

        schedule.setStatus(status);
        if (status == ProductionSchedule.ScheduleStatus.IN_PROGRESS) {
            schedule.setActualStartTime(LocalDateTime.now());
        } else if (status == ProductionSchedule.ScheduleStatus.COMPLETED) {
            schedule.setActualEndTime(LocalDateTime.now());
        }

        productionScheduleRepository.save(schedule);
        log.info("Updated schedule {} status to {}", scheduleId, status);
    }

    /**
     * Deletes all schedules for a work order (used when canceling work order).
     *
     * @param workOrderId ID of the work order
     */
    @Transactional
    public void deleteSchedulesForWorkOrder(Long workOrderId) {
        List<ProductionSchedule> schedules = getSchedulesForWorkOrder(workOrderId);
        productionScheduleRepository.deleteAll(schedules);
        log.info("Deleted {} schedules for workOrder={}", schedules.size(), workOrderId);
    }
}
