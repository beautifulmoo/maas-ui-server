package com.maas.ui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NetworkProfileService {
    
    private static final String PROFILES_FILE_NAME = "maas-network-profiles.json";
    private final ObjectMapper objectMapper;
    private final Path profilesFilePath;
    
    public NetworkProfileService() {
        this.objectMapper = new ObjectMapper();
        // 프로젝트 루트 디렉토리에 profiles 파일 저장
        String projectRoot = System.getProperty("user.dir");
        this.profilesFilePath = Paths.get(projectRoot, PROFILES_FILE_NAME);
    }
    
    /**
     * 네트워크 프로파일 목록을 파일에서 로드합니다.
     * @return 프로파일 목록, 파일이 없거나 오류가 발생하면 빈 리스트 반환
     */
    public List<Map<String, Object>> loadProfiles() {
        try {
            if (Files.exists(profilesFilePath)) {
                String content = Files.readString(profilesFilePath);
                List<Map<String, Object>> profiles = objectMapper.readValue(content, 
                    new TypeReference<List<Map<String, Object>>>() {});
                return profiles;
            }
        } catch (IOException e) {
            System.err.println("Error loading network profiles file: " + e.getMessage());
        }
        
        // 빈 리스트 반환
        return new ArrayList<>();
    }
    
    /**
     * 네트워크 프로파일 목록을 파일에 저장합니다.
     * @param profiles 저장할 프로파일 목록
     * @return 저장 성공 여부
     */
    public boolean saveProfiles(List<Map<String, Object>> profiles) {
        try {
            // 디렉토리가 없으면 생성
            if (profilesFilePath.getParent() != null) {
                Files.createDirectories(profilesFilePath.getParent());
            }
            
            // JSON 형식으로 저장
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profiles);
            Files.writeString(profilesFilePath, json);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving network profiles file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 프로파일 파일 경로를 반환합니다.
     * @return 프로파일 파일 경로
     */
    public String getProfilesFilePath() {
        return profilesFilePath.toString();
    }
}
