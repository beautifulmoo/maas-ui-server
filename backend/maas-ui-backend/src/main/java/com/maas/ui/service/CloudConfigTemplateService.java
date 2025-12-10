package com.maas.ui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class CloudConfigTemplateService {
    
    private static final String TEMPLATES_FILE_NAME = "maas-cloud-config-templates.json";
    private final ObjectMapper objectMapper;
    private final Path templatesFilePath;
    
    public CloudConfigTemplateService() {
        this.objectMapper = new ObjectMapper();
        // 프로젝트 루트 디렉토리에 templates 파일 저장
        String projectRoot = System.getProperty("user.dir");
        this.templatesFilePath = Paths.get(projectRoot, TEMPLATES_FILE_NAME);
    }
    
    /**
     * Cloud-Config 템플릿 목록을 파일에서 로드합니다.
     * @return 템플릿 목록, 파일이 없거나 오류가 발생하면 빈 리스트 반환
     */
    public List<Map<String, Object>> loadTemplates() {
        try {
            if (Files.exists(templatesFilePath)) {
                String content = Files.readString(templatesFilePath);
                List<Map<String, Object>> templates = objectMapper.readValue(content, 
                    new TypeReference<List<Map<String, Object>>>() {});
                // Migrate old format (tag -> tags)
                return migrateTemplates(templates);
            }
        } catch (IOException e) {
            System.err.println("Error loading cloud-config templates file: " + e.getMessage());
        }
        
        // 빈 리스트 반환
        return new ArrayList<>();
    }
    
    /**
     * Cloud-Config 템플릿 목록을 파일에 저장합니다.
     * @param templates 저장할 템플릿 목록
     * @return 저장 성공 여부
     */
    public boolean saveTemplates(List<Map<String, Object>> templates) {
        try {
            // 디렉토리가 없으면 생성
            if (templatesFilePath.getParent() != null) {
                Files.createDirectories(templatesFilePath.getParent());
            }
            
            // 각 템플릿에 base64 인코딩된 값 추가
            List<Map<String, Object>> templatesWithBase64 = new ArrayList<>();
            for (Map<String, Object> template : templates) {
                Map<String, Object> templateWithBase64 = new java.util.HashMap<>(template);
                
                // cloudConfig가 있으면 base64 인코딩된 값도 추가
                if (template.containsKey("cloudConfig") && template.get("cloudConfig") != null) {
                    String cloudConfig = template.get("cloudConfig").toString();
                    if (!cloudConfig.trim().isEmpty()) {
                        // Base64 인코딩 (JavaScript의 btoa(unescape(encodeURIComponent()))와 동일)
                        String base64Encoded = Base64.getEncoder().encodeToString(
                            cloudConfig.getBytes(StandardCharsets.UTF_8)
                        );
                        templateWithBase64.put("cloudConfigBase64", base64Encoded);
                    }
                }
                
                templatesWithBase64.add(templateWithBase64);
            }
            
            // JSON 형식으로 저장
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(templatesWithBase64);
            Files.writeString(templatesFilePath, json);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error saving cloud-config templates file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 기존 형식 (tag -> tags) 마이그레이션 및 base64 인코딩 값 추가
     */
    private List<Map<String, Object>> migrateTemplates(List<Map<String, Object>> templates) {
        boolean needsMigration = false;
        List<Map<String, Object>> migrated = new ArrayList<>();
        
        for (Map<String, Object> template : templates) {
            Map<String, Object> migratedTemplate = new java.util.HashMap<>(template);
            
            // Old format: single tag -> tags array
            if (template.containsKey("tag") && !template.containsKey("tags")) {
                Object tagValue = template.get("tag");
                if (tagValue != null) {
                    migratedTemplate.put("tags", java.util.Arrays.asList(tagValue));
                } else {
                    migratedTemplate.put("tags", new ArrayList<>());
                }
                migratedTemplate.remove("tag");
                needsMigration = true;
            } else if (!template.containsKey("tags")) {
                // No tags at all
                migratedTemplate.put("tags", new ArrayList<>());
                needsMigration = true;
            }
            
            // cloudConfigBase64가 없으면 생성
            if (template.containsKey("cloudConfig") && template.get("cloudConfig") != null 
                    && !template.containsKey("cloudConfigBase64")) {
                String cloudConfig = template.get("cloudConfig").toString();
                if (!cloudConfig.trim().isEmpty()) {
                    String base64Encoded = Base64.getEncoder().encodeToString(
                        cloudConfig.getBytes(StandardCharsets.UTF_8)
                    );
                    migratedTemplate.put("cloudConfigBase64", base64Encoded);
                    needsMigration = true;
                }
            }
            
            migrated.add(migratedTemplate);
        }
        
        // 마이그레이션이 필요하면 파일에 저장
        if (needsMigration) {
            // saveTemplates를 호출하면 다시 base64 인코딩을 추가하므로, 여기서는 직접 저장
            try {
                if (templatesFilePath.getParent() != null) {
                    Files.createDirectories(templatesFilePath.getParent());
                }
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(migrated);
                Files.writeString(templatesFilePath, json);
            } catch (IOException e) {
                System.err.println("Error saving migrated templates: " + e.getMessage());
            }
        }
        
        return migrated;
    }
    
    /**
     * 템플릿 파일 경로를 반환합니다.
     * @return 템플릿 파일 경로
     */
    public String getTemplatesFilePath() {
        return templatesFilePath.toString();
    }
}

