package com.smartmes.backend.modules.production.entity;

import com.smartmes.backend.core.common.BaseEntity;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
public class WorkOrder extends BaseEntity {

    // Unique Order Number (e.g., WO-20260306-001)
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    // Reference to the Product being produced
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    // Quantity to produce
    @Column(name = "planned_quantity", nullable = false)
    private Integer plannedQuantity;

    // Actual quantity finished (Updated during production)
    @Column(name = "actual_quantity")
    private Integer actualQuantity = 0;

    // Scheduled Start & End dates
    @Column(name = "planned_start_date")
    private LocalDateTime plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDateTime plannedEndDate;

    // Current status of the order
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    // Priority level
    @Column(name = "priority")
    private Integer priority = 1; // 1: Low, 2: Medium, 3: High

    public enum WorkOrderStatus {
        DRAFT,      // Created but not released to shop floor
        RELEASED,   // Ready to start production
        IN_PROGRESS,// Currently being produced
        COMPLETED,  // Finished all quantities
        CANCELLED   // Order stopped/aborted
    }
}