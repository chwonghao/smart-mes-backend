package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    List<Worker> findByTenantIdOrderByIdDesc(String tenantId);
    boolean existsByWorkerCodeAndTenantId(String workerCode, String tenantId);
}