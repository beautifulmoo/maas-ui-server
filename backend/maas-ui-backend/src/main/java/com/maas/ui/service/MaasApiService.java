package com.maas.ui.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

@Service
public class MaasApiService {
    
    @Autowired
    private MaasAuthService authService;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public MaasApiService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * MAAS 서버에서 모든 머신 정보를 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return 머신 정보가 담긴 Map
     */
    public Mono<Map<String, Object>> getAllMachines(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("MAAS API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        
                        // MAAS API는 직접 배열을 반환하거나 {"results": [...]} 형태로 반환할 수 있음
                        if (jsonNode.isArray()) {
                            // 직접 배열 형태인 경우
                            System.out.println("Direct array response found: " + jsonNode.size() + " machines");
                            result.put("results", jsonNode);
                            result.put("count", jsonNode.size());
                        } else if (jsonNode.has("results")) {
                            // {"results": [...]} 형태인 경우
                            JsonNode results = jsonNode.get("results");
                            System.out.println("Results field found: " + results.size() + " machines");
                            result.put("results", results);
                            if (jsonNode.has("count")) {
                                result.put("count", jsonNode.get("count").asInt());
                            } else {
                                result.put("count", results.size());
                            }
                        } else {
                            System.out.println("Unexpected response format");
                            result.put("results", objectMapper.createArrayNode());
                            result.put("count", 0);
                        }
                        
                        System.out.println("Final result: " + result);
                        return result;
                    } catch (Exception e) {
                        System.out.println("JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버 연결을 테스트합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return 연결 테스트 결과
     */
    public Mono<Map<String, Object>> testConnection(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/version/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에 새 머신을 추가합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param hostname 호스트 이름 (선택사항)
     * @param architecture 아키텍처
     * @param macAddresses MAC 주소
     * @param powerType 전원 타입
     * @param commission 커미셔닝 여부
     * @param description 설명 (선택사항)
     * @return 머신 추가 결과
     */
    public Mono<Map<String, Object>> addMachine(String maasUrl, String apiKey, 
            String hostname, String architecture, String macAddresses, 
            String powerType, String commission, String description) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/";
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .bodyValue(createFormData(hostname, architecture, macAddresses, 
                        powerType, commission, description))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Add Machine API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Add Machine JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신을 커미셔닝합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param skipBmcConfig BMC 설정 건너뛰기 여부
     * @return 커미셔닝 결과
     */
    public Mono<Map<String, Object>> commissionMachine(String maasUrl, String apiKey, 
            String systemId, String skipBmcConfig) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-commission";
        
        Map<String, String> formData = new HashMap<>();
        formData.put("skip_bmc_config", skipBmcConfig);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Commission Machine API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Commission Machine JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * 폼 데이터를 생성합니다.
     */
    private Map<String, String> createFormData(String hostname, String architecture, 
            String macAddresses, String powerType, String commission, String description) {
        Map<String, String> formData = new HashMap<>();
        
        if (hostname != null && !hostname.trim().isEmpty()) {
            formData.put("hostname", hostname.trim());
        }
        formData.put("architecture", architecture);
        formData.put("mac_addresses", macAddresses);
        formData.put("power_type", powerType);
        formData.put("commission", commission);
        if (description != null && !description.trim().isEmpty()) {
            formData.put("description", description.trim());
        }
        
        return formData;
    }
    
    /**
     * 머신 통계를 계산합니다.
     * 
     * @param machinesData 머신 데이터
     * @return 통계 정보
     */
    public Map<String, Integer> calculateMachineStats(Map<String, Object> machinesData) {
        int totalMachines = 0;
        int commissionedMachines = 0;
        int deployedMachines = 0;
        
        if (machinesData != null && machinesData.containsKey("results")) {
            try {
                JsonNode results = (JsonNode) machinesData.get("results");
                if (results.isArray()) {
                    totalMachines = results.size();
                    
                    for (JsonNode machine : results) {
                        if (machine.has("status")) {
                            int status = machine.get("status").asInt();
                            if (status == 4) { // Commissioned
                                commissionedMachines++;
                            } else if (status == 6) { // Deployed
                                deployedMachines++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 에러 발생 시 기본값 사용
            }
        }
        
        return Map.of(
            "total", totalMachines,
            "commissioned", commissionedMachines,
            "deployed", deployedMachines
        );
    }
}
