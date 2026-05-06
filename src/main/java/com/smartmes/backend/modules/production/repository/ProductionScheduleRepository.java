package com.smartmes.backend.modules.production.repository;

import com.smartmes.backend.modules.production.entity.ProductionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionScheduleRepository extends JpaRepository<ProductionSchedule, Long> {
    
    List<ProductionSchedule> findByWorkOrderIdOrderBySequenceNumber(Long workOrderId);
    
    List<ProductionSchedule> findByWorkCenterId(Long workCenterId);
}
