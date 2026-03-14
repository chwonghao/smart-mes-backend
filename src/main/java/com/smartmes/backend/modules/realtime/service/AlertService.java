package com.smartmes.backend.modules.realtime.service;

import com.smartmes.backend.modules.realtime.dto.AlertNotificationDto;
import com.smartmes.backend.modules.realtime.entity.SystemAlert;
import com.smartmes.backend.modules.realtime.repository.SystemAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final SystemAlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createAndSendAlert(String type, String message, String tenantId) {
        // 1. Lưu vào Database
        SystemAlert alert = new SystemAlert();
        alert.setAlertType(type);
        alert.setMessage(message);
        alert.setTenantId(tenantId);
        alertRepository.save(alert);

        // 2. Bắn qua WebSocket cho Client
        AlertNotificationDto dto = new AlertNotificationDto(type, message, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/alerts", dto);
    }

    public List<SystemAlert> getUnreadAlerts(String tenantId) {
        return alertRepository.findByTenantIdAndIsReadFalseOrderByCreatedAtDesc(tenantId);
    }

    public List<SystemAlert> getAllAlerts(String tenantId) {
        return alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void markAsRead(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setRead(true);
            alertRepository.save(alert);
        });
    }
}