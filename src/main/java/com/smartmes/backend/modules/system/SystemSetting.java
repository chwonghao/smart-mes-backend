package com.smartmes.backend.modules.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 50)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "description")
    private String description;
}