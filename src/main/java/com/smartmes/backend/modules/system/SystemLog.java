package com.smartmes.backend.modules.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Immutable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50, updatable = false)
    private String tenantId;

    @Column(name = "module_name", nullable = false, length = 100, updatable = false)
    private String module;

    @Column(name = "action_type", nullable = false, length = 50, updatable = false)
    private String actionType;

    @Column(name = "target_id", length = 100, updatable = false)
    private String targetId;

    @Column(name = "description", length = 1000, updatable = false)
    private String description;

    @Column(name = "old_value", columnDefinition = "jsonb", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
