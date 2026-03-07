package com.smartmes.backend.modules.realtime.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_alerts")
@Getter
@Setter
public class SystemAlert extends BaseEntity {

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType; // VD: QC_ALERT, INVENTORY_ALERT, MACHINE_DOWN

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private boolean isRead = false; // Trạng thái đã đọc hay chưa
}