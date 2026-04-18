package com.smartmes.backend.modules.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findByTenantIdAndActiveTrueOrderByIdDesc(String tenantId);
    Optional<UserAccount> findByIdAndTenantId(Long id, String tenantId);
}