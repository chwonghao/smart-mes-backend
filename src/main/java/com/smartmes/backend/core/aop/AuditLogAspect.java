package com.smartmes.backend.core.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmes.backend.core.annotation.AuditLog;
import com.smartmes.backend.core.security.SecurityUtils;
import com.smartmes.backend.modules.system.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void logAfter(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            String tenantId = SecurityUtils.getCurrentTenantId();
            String username = getCurrentUsername();
            Object[] args = joinPoint.getArgs();
            String argsJson = objectMapper.writeValueAsString(args);

            String detailedDescription = String.format(
                    "%s | user=%s | tenant=%s | method=%s | payload=%s",
                    auditLog.description(),
                    username,
                    tenantId,
                    joinPoint.getSignature().toShortString(),
                    argsJson
            );

            systemLogService.logAction(
                    auditLog.module(),
                    auditLog.actionType(),
                    null,
                    detailedDescription,
                    null,
                    argsJson
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize method arguments for audit log", ex);
        } catch (Exception ex) {
            // Never interrupt successful business flow because of audit logging.
            log.error("Failed to process @AuditLog aspect", ex);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
