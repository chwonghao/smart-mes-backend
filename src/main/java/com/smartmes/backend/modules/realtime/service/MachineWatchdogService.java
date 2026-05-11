package com.smartmes.backend.modules.realtime.service;

import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MachineWatchdogService {

    private final WorkCenterRepository workCenterRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkOfflineMachines() {
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);

        List<WorkCenter> offlineMachines = workCenterRepository.findMachinesToMarkOffline(
            threeMinutesAgo,
            WorkCenter.MachineStatus.OFFLINE
        );

        if (offlineMachines.isEmpty()) {
            return;
        }

        List<WorkCenter> updatedMachines = new ArrayList<>(offlineMachines.size());
        for (WorkCenter wc : offlineMachines) {
            log.info(
                "Machine watchdog detected offline machine: id={}, name={}, lastPingAt={}",
                wc.getId(),
                wc.getName(),
                wc.getLastPingAt()
            );
            wc.setCurrentStatus(WorkCenter.MachineStatus.OFFLINE);
            updatedMachines.add(wc);
        }

        workCenterRepository.saveAll(updatedMachines);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                updatedMachines.forEach(MachineWatchdogService.this::broadcastOfflineAlert);
            }
        });
    }

    private void broadcastOfflineAlert(WorkCenter workCenter) {
        MachineOfflineAlertPayload payload = new MachineOfflineAlertPayload(
            "MACHINE_OFFLINE_ALERT",
            workCenter.getId(),
            workCenter.getName(),
            LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/dashboard", payload);
        messagingTemplate.convertAndSend("/topic/alerts", payload);
    }

    private record MachineOfflineAlertPayload(
        String type,
        Long workCenterId,
        String workCenterName,
        LocalDateTime timestamp
    ) {
    }
}