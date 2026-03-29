package com.smartmes.backend.modules.masterdata.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "master_workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Worker extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String workerCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role; // WORKER, QC, LEADER

    private String shift; // MORNING, NIGHT
    private String status; // ACTIVE, INACTIVE
}