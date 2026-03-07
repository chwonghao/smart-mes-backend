package com.smartmes.backend.modules.production.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "production_logs")
@Getter
@Setter
public class ProductionLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "quantity_done", nullable = false)
    private Integer quantityDone;

    @OneToOne(mappedBy = "productionLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private QualityCheck qualityCheck;

    @Column(name = "notes")
    private String notes;

    @Column(name = "operator_name")
    private String operatorName; // Tên công nhân báo cáo
}