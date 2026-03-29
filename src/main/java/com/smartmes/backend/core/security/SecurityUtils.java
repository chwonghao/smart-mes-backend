package com.smartmes.backend.core.security;

import com.smartmes.backend.modules.auth.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }

        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object tenant = map.get("tenantId");
            if (tenant instanceof String tenantId && !tenantId.isBlank()) {
                return tenantId;
            }
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserAccount account
                && account.getTenantId() != null
                && !account.getTenantId().isBlank()) {
            return account.getTenantId();
        }

        throw new IllegalStateException("TenantId not found in SecurityContext");
    }
}
