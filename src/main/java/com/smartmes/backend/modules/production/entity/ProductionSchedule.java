package com.smartmes.backend.modules.production.entity;

import com.smartmes.backend.core.common.BaseEntity;
import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_schedules", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"work_order_id", "work_center_id"}))
@Getter
@Setter
public class ProductionSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_center_id", nullable = false)
    private WorkCenter workCenter;

    // Thứ tự công đoạn (lấy từ Routing step number)
    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    // Số lượng cần làm trên máy này
    @Column(name = "quantity_target")
    private Integer quantityTarget;

    // Số lượng đã làm
    @Column(name = "quantity_completed")
    private Integer quantityCompleted = 0;

    @Transient
    public double getCompletionPercentage() {
        if (quantityTarget == null || quantityTarget <= 0 || quantityCompleted == null) {
            return 0.0;
        }

        double percentage = ((double) quantityCompleted / quantityTarget) * 100.0;
        return Math.round(percentage * 100.0) / 100.0;
    }

    // Trạng thái lịch sản xuất
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    // Thời gian dự kiến
    @Column(name = "estimated_start_time")
    private LocalDateTime estimatedStartTime;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    // Thời gian thực tế
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    public enum ScheduleStatus {
        PENDING,      // Chưa bắt đầu
        IN_PROGRESS,  // Đang thực hiện
        COMPLETED,    // Hoàn thành
        SKIPPED       // Bỏ qua
    }
}
