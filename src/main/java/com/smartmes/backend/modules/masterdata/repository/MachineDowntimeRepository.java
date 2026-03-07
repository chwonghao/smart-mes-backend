package com.smartmes.backend.modules.masterdata.repository;

import com.smartmes.backend.modules.masterdata.entity.MachineDowntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineDowntimeRepository extends JpaRepository<MachineDowntime, Long> {
    // Tìm sự cố đang diễn ra (chưa có endTime) của một máy
    Optional<MachineDowntime> findByWorkCenterIdAndEndTimeIsNull(Long workCenterId);
}