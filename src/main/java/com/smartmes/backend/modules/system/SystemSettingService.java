package com.smartmes.backend.modules.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SettingRepository settingRepository;

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return settingRepository.findById(key)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(defaultValue);
    }
    
    public double getDoubleSetting(String key, double defaultValue) {
        return settingRepository.findById(key)
                .map(setting -> Double.parseDouble(setting.getSettingValue()))
                .orElse(defaultValue);
    }
}