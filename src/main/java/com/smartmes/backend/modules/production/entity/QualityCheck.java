package com.smartmes.backend.modules.production.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quality_checks")
@Getter
@Setter
public class QualityCheck extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_log_id", nullable = false)
    private ProductionLog productionLog; // Gắn trực tiếp vào lần báo cáo sản lượng

    @Column(name = "passed_quantity", nullable = false)
    private Integer passedQuantity; // Số lượng đạt chuẩn

    @Column(name = "failed_quantity", nullable = false)
    private Integer failedQuantity; // Số lượng lỗi (NG)

    @Column(name = "defect_reason")
    private String defectReason; // Lý do lỗi (vỡ, trầy xước, sai kích thước...)

    @Column(name = "inspector_name")
    private String inspectorName; // Người kiểm tra
}