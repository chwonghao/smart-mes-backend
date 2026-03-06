package com.smartmes.backend.modules.masterdata.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "routing")
@Getter
@Setter
public class Routing extends BaseEntity {

    // The product that follows this routing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    // Sequence of the operation (e.g., 10, 20, 30)
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    // Name of the operation (e.g., Cutting, Sanding, Painting)
    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    // Where this operation takes place
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_center_id", nullable = false)
    private WorkCenter workCenter;

    // Estimated time to finish 1 unit (in minutes)
    @Column(name = "standard_time", nullable = false)
    private Integer standardTime;

    // Description of what to do in this step
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}