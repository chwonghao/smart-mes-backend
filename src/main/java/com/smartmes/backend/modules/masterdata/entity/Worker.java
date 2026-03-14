package com.smartmes.backend.modules.masterdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "master_workers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String workerCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role; // WORKER, QC, LEADER

    private String shift; // MORNING, NIGHT
    private String status; // ACTIVE, INACTIVE

    private String tenantId;
    
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}