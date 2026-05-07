package com.smartmes.backend.modules.production.dto;

import com.smartmes.backend.modules.production.entity.ProductionSchedule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductionScheduleDto {
    private Long id;
    private Long workOrderId;
    private Long workCenterId;
    private String workCenterName;
    private Integer sequenceNumber;
    private Integer quantityTarget;
    private Integer quantityCompleted;
    private Double completionPercentage;
    private String status;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime estimatedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    public static ProductionScheduleDto fromEntity(ProductionSchedule entity) {
        return ProductionScheduleDto.builder()
                .id(entity.getId())
                .workOrderId(entity.getWorkOrder().getId())
                .workCenterId(entity.getWorkCenter().getId())
                .workCenterName(entity.getWorkCenter().getName())
                .sequenceNumber(entity.getSequenceNumber())
                .quantityTarget(entity.getQuantityTarget())
                .quantityCompleted(entity.getQuantityCompleted())
                .completionPercentage(entity.getCompletionPercentage())
                .status(entity.getStatus().name())
                .estimatedStartTime(entity.getEstimatedStartTime())
                .estimatedEndTime(entity.getEstimatedEndTime())
                .actualStartTime(entity.getActualStartTime())
                .actualEndTime(entity.getActualEndTime())
                .build();
    }
}
