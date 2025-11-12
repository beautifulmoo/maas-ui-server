package com.maas.ui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class SettingsService {
    
    private static final String SETTINGS_FILE_NAME = "maas-ui-settings.json";
    private final ObjectMapper objectMapper;
    private final Path settingsFilePath;
    
    public SettingsService() {
        this.objectMapper = new ObjectMapper();
        // 프로젝트 루트 디렉토리에 settings 파일 저장
        String projectRoot = System.getProperty("user.dir");
        this.settingsFilePath = Paths.get(projectRoot, SETTINGS_FILE_NAME);
    }
    
    /**
     * 설정을 파일에서 로드합니다.
     * @return 설정 맵, 파일이 없거나 오류가 발생하면 기본값 반환
     */
    public Map<String, Object> loadSettings() {
        try {
            if (Files.exists(settingsFilePath)) {
                String content = Files.readString(settingsFilePath);
                Map<String, Object> settings = objectMapper.readValue(content, 
                    new TypeReference<Map<String, Object>>() {});
                return settings;
            }
        } catch (IOException e) {
            System.err.println("Error loading settings file: " + e.getMessage());
        }
        
        // 기본값 반환
        return getDefaultSettings();
    }
    
    /**
     * 설정을 파일에 저장합니다.
     * @param settings 저장할 설정 맵
     * @return 저장 성공 여부
     */
    public boolean saveSettings(Map<String, Object> settings) {
        try {
            // 디렉토리가 없으면 생성
            if (settingsFilePath.getParent() != null) {
                Files.createDirectories(settingsFilePath.getParent());
            }
            
            // JSON 형식으로 저장
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
            Files.writeString(settingsFilePath, json);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving settings file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 기본 설정값을 반환합니다.
     * @return 기본 설정 맵
     */
    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("maasUrl", "http://192.168.189.71:5240");
        defaults.put("apiKey", "idwbvWv1cxnNajGSaX:nBFwezD08jUznN0pMA:VcrKMpOquA0C31egWGM07kxDEZTXVAiF");
        defaults.put("refreshInterval", 30);
        defaults.put("autoRefresh", true);
        defaults.put("itemsPerPage", 25);
        defaults.put("showAdvancedInfo", false);
        return defaults;
    }
    
    /**
     * 설정 파일 경로를 반환합니다.
     * @return 설정 파일 경로
     */
    public String getSettingsFilePath() {
        return settingsFilePath.toString();
    }
}

