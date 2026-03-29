package com.smartmes.backend.modules.system;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SettingRepository settingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Always fetches latest value from DB (no @Cacheable on this method).
     */
    public String getSettingValue(String key) {
        return settingRepository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    public String getSettingValue(String key, String defaultValue) {
        String value = getSettingValue(key);
        return value != null ? value : defaultValue;
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getSettingValue(key, String.valueOf(defaultValue)));
    }
    
    public double getDoubleSetting(String key, double defaultValue) {
        String value = getSettingValue(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Transactional
    @CacheEvict(value = "settings", allEntries = true)
    public void updateSetting(String key, String value) {
        SystemSetting setting = settingRepository.findById(key).orElse(new SystemSetting());
        setting.setSettingKey(key);
        setting.setSettingValue(String.valueOf(value));
        settingRepository.save(setting);
    }

    @Transactional
    @CacheEvict(value = "settings", allEntries = true)
    public void saveSettings(Map<String, String> settings) {
        settings.forEach(this::updateSetting);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "SETTINGS_UPDATED");
        payload.put("timestamp", LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/dashboard", payload);
    }
}