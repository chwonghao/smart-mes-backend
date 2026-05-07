package com.smartmes.backend.modules.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    @EntityGraph(attributePaths = "workCenter")
    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = "workCenter")
    List<UserAccount> findByTenantIdAndActiveTrueOrderByIdDesc(String tenantId);

    @EntityGraph(attributePaths = "workCenter")
    Optional<UserAccount> findByIdAndTenantId(Long id, String tenantId);
}