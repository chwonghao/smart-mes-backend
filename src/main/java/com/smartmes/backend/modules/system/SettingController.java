package com.smartmes.backend.modules.system;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chặn bảo mật: Chỉ Admin được đổi cấu hình
public class SettingController {

    private final SettingRepository settingRepository;

    // Lấy toàn bộ cấu hình dưới dạng Key-Value (Ví dụ: { "FACTORY_NAME": "SmartMES" })
    @GetMapping
    public Map<String, String> getAllSettings() {
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    // Lưu hàng loạt cấu hình
    @PostMapping
    public void saveSettings(@RequestBody Map<String, String> settings) {
        settings.forEach((key, value) -> {
            SystemSetting setting = settingRepository.findById(key).orElse(new SystemSetting());
            setting.setSettingKey(key);
            // Ép kiểu mọi thứ về String để lưu vào Database
            setting.setSettingValue(String.valueOf(value)); 
            settingRepository.save(setting);
        });
    }
}