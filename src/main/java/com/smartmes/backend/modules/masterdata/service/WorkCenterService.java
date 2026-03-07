package com.smartmes.backend.modules.masterdata.service;

import com.smartmes.backend.modules.masterdata.dto.WorkCenterRequestDto;
import com.smartmes.backend.modules.masterdata.dto.WorkCenterResponseDto;
import com.smartmes.backend.modules.masterdata.entity.MachineDowntime;
import com.smartmes.backend.modules.masterdata.entity.WorkCenter;
import com.smartmes.backend.modules.masterdata.repository.MachineDowntimeRepository;
import com.smartmes.backend.modules.masterdata.repository.WorkCenterRepository;
import com.smartmes.backend.modules.realtime.service.AlertService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkCenterService {

    private final WorkCenterRepository repository;
    private final MachineDowntimeRepository downtimeRepository;
    private final AlertService alertService; // Tích hợp Real-time Alert!

    @Transactional
    public WorkCenterResponseDto createWorkCenter(WorkCenterRequestDto dto, String tenantId) {
        // 1. Prevent duplicate work center codes
        if (repository.existsByCodeAndTenantId(dto.getCode(), tenantId)) {
            throw new IllegalArgumentException("Work Center code '" + dto.getCode() + "' already exists!");
        }

        // 2. Map DTO to Entity
        WorkCenter workCenter = new WorkCenter();
        workCenter.setCode(dto.getCode());
        workCenter.setName(dto.getName());
        workCenter.setCenterType(WorkCenter.CenterType.valueOf(dto.getCenterType()));
        workCenter.setHourlyCapacity(dto.getHourlyCapacity());
        workCenter.setActive(dto.isActive());
        
        workCenter.setTenantId(tenantId);
        workCenter.setCreatedBy("ADMIN");

        // 3. Save to database
        WorkCenter savedWorkCenter = repository.save(workCenter);

        // 4. Return mapped Response DTO
        return mapToResponseDto(savedWorkCenter);
    }

    // Get all work centers (including inactive ones for management)
    public List<WorkCenterResponseDto> getAllWorkCenters(String tenantId) {
        return repository.findByTenantIdAndIsDeletedFalse(tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // Get ONLY active work centers (for Production Scheduling dropdowns)
    public List<WorkCenterResponseDto> getActiveWorkCenters(String tenantId) {
        return repository.findByTenantIdAndIsActiveTrueAndIsDeletedFalse(tenantId)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private WorkCenterResponseDto mapToResponseDto(WorkCenter workCenter) {
        return WorkCenterResponseDto.builder()
                .id(workCenter.getId())
                .code(workCenter.getCode())
                .name(workCenter.getName())
                .centerType(workCenter.getCenterType().name())
                .hourlyCapacity(workCenter.getHourlyCapacity())
                .isActive(workCenter.isActive())
                .build();
    }
    
    @Transactional
    public void reportMachineDown(Long workCenterId, String reason, String tenantId) {
        WorkCenter wc = repository.findById(workCenterId)
                .orElseThrow(() -> new RuntimeException("Work Center not found"));

        if (wc.getCurrentStatus() == WorkCenter.MachineStatus.DOWN) {
            throw new IllegalStateException("Máy này đã được báo hỏng trước đó rồi!");
        }

        // 1. Cập nhật trạng thái máy
        wc.setCurrentStatus(WorkCenter.MachineStatus.DOWN);
        repository.save(wc);

        // 2. Lưu lịch sử sự cố
        MachineDowntime downtime = new MachineDowntime();
        downtime.setWorkCenter(wc);
        downtime.setStartTime(LocalDateTime.now());
        downtime.setReason(reason);
        downtime.setReportedBy("WORKER_02");
        downtime.setTenantId(tenantId);
        downtimeRepository.save(downtime);

        // 3. Gửi cảnh báo TỨC THỜI cho quản đốc & đội bảo trì
        String alertMsg = String.format("KHẨN CẤP: Máy [%s] vừa ngừng hoạt động. Lý do: %s", wc.getName(), reason);
        alertService.createAndSendAlert("MACHINE_DOWN", alertMsg, tenantId);
    }

    @Transactional
    public void resolveMachineIssue(Long workCenterId, String tenantId) {
        WorkCenter wc = repository.findById(workCenterId)
                .orElseThrow(() -> new RuntimeException("Work Center not found"));

        // Tìm sự cố chưa được giải quyết của máy này
        MachineDowntime downtime = downtimeRepository.findByWorkCenterIdAndEndTimeIsNull(workCenterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố nào đang diễn ra cho máy này."));

        // 1. Chốt thời gian kết thúc sự cố
        downtime.setEndTime(LocalDateTime.now());
        downtimeRepository.save(downtime);

        // 2. Chuyển trạng thái máy về IDLE (sẵn sàng sản xuất tiếp)
        wc.setCurrentStatus(WorkCenter.MachineStatus.IDLE);
        repository.save(wc);

        // 3. Bắn thông báo đã sửa xong
        String alertMsg = String.format("TIN VUI: Máy [%s] đã được sửa chữa xong và sẵn sàng hoạt động.", wc.getName());
        alertService.createAndSendAlert("MACHINE_FIXED", alertMsg, tenantId);
    }
}