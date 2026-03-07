package com.smartmes.backend.modules.masterdata.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_downtimes")
@Getter
@Setter
public class MachineDowntime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_center_id", nullable = false)
    private WorkCenter workCenter;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime; // Sẽ được cập nhật khi máy sửa xong

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Lý do hỏng (VD: Kẹt motor, đứt dây curoa...)

    @Column(name = "reported_by")
    private String reportedBy; // Người báo sự cố
}