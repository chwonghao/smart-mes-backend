package com.smartmes.backend.modules.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmes.backend.core.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String module,
                          String actionType,
                          String targetId,
                          String description,
                          Object oldValue,
                          Object newValue) {
        try {
            String tenantId = SecurityUtils.getCurrentTenantId();
            String username = getCurrentUsername();
            String ipAddress = getCurrentIpAddress();

            SystemLog systemLog = SystemLog.builder()
                    .tenantId(tenantId)
                    .module(module)
                    .actionType(actionType)
                    .targetId(targetId)
                    .description(description)
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .ipAddress(ipAddress)
                    .createdBy(username)
                    .createdAt(LocalDateTime.now())
                    .build();

            systemLogRepository.save(systemLog);
        } catch (Exception ex) {
            // Logging failure must never break primary business flow.
            log.error("Failed to write system audit log", ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<SystemLog> getLogs(String module,
                                   LocalDateTime fromTime,
                                   LocalDateTime toTime,
                                   Pageable pageable) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        String normalizedModule = (module == null || module.isBlank()) ? null : module;
        return systemLogRepository.searchLogs(tenantId, normalizedModule, fromTime, toTime, pageable);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit log payload", ex);
            return null;
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String getCurrentIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
