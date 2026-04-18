package com.smartmes.backend.modules.realtime.service;

import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineWatchdogService {

    private final WorkCenterRepository workCenterRepository;
    private final AlertService alertService;

    // Chạy ngầm mỗi 60 giây (60000 ms)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOfflineMachines() {
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);

        // Đẩy toàn bộ điều kiện lọc xuống DB để tránh scan + filter in-memory.
        List<WorkCenter> offlineMachines = workCenterRepository.findMachinesToMarkOffline(
            threeMinutesAgo,
            WorkCenter.MachineStatus.OFFLINE
        );

        for (WorkCenter wc : offlineMachines) {
            wc.setCurrentStatus(WorkCenter.MachineStatus.OFFLINE);
            workCenterRepository.save(wc);

            // Bắn thông báo Real-time đỏ chót lên Dashboard
            String alertMsg = String.format("MẤT KẾT NỐI: Máy [%s] không phản hồi trong 3 phút qua. Vui lòng kiểm tra mạng!", wc.getName());
            alertService.createAndSendAlert("MACHINE_OFFLINE", alertMsg, wc.getTenantId());
        }
    }
}