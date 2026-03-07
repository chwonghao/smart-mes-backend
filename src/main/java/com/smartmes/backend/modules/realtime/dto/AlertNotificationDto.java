package com.smartmes.backend.modules.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AlertNotificationDto {
    private String type; // VD: "QC_ALERT", "STOCK_ALERT"
    private String message;
    private LocalDateTime timestamp;
}