package com.smartmes.backend.modules.masterdata.entity;

import java.time.LocalDateTime;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "work_center")
@Getter
@Setter
public class WorkCenter extends BaseEntity {

    // Unique code for the work center (e.g., CNC-01, LINE-A)
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    // Descriptive name (e.g., CNC Milling Machine 01)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // Type of the work center
    @Enumerated(EnumType.STRING)
    @Column(name = "center_type", nullable = false, length = 50)
    private CenterType centerType;

    // Standard hourly capacity (e.g., how many items it can process per hour)
    // Useful for the Scheduling/Planning module later
    @Column(name = "hourly_capacity", nullable = false)
    private Integer hourlyCapacity;

    // Status to indicate if the machine is active or under maintenance
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Definition of Work Center types
    public enum CenterType {
        MACHINE,        // A single machine (e.g., Lathe, CNC)
        ASSEMBLY_LINE,  // A progressive assembly line
        WORKSTATION,    // A manual workbench
        PACKAGING       // Packaging and labeling area
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 20)
    private MachineStatus currentStatus = MachineStatus.IDLE;

    // ĐÃ BỔ SUNG: Cột lưu thời gian ping cuối cùng để Watchdog kiểm tra
    @Column(name = "last_ping_at")
    private LocalDateTime lastPingAt = LocalDateTime.now();

    public enum MachineStatus {
        IDLE,       // Máy đang rảnh
        RUNNING,    // Máy đang chạy (sản xuất)
        DOWN,       // Máy đang hỏng/sự cố
        MAINTENANCE, // Máy đang bảo trì định kỳ
        OFFLINE     // Máy đang mất kết nối
    }
}