package com.maas.ui.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
     * MAAS 서버에서 개별 머신 정보를 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return 머신 정보가 담긴 Map
     */
    public Mono<Map<String, Object>> getMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        return result;
                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Failed to fetch machine: " + e.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신의 커미셔닝을 중단합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return 중단 결과
     */
    public Mono<Map<String, Object>> abortCommissioning(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-abort";
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", "Failed to abort commissioning: " + e.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신을 릴리스합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return 릴리스 결과
     */
    public Mono<Map<String, Object>> releaseMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-release";
        
        System.out.println("Release Machine - URL: " + url);
        System.out.println("Release Machine - systemId: " + systemId);
        
        Map<String, String> formData = new HashMap<>();
        formData.put("force", "true");
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .bodyValue(formData)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Release Machine API Error Response: " + errorBody);
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    return Mono.error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException(errorBody));
                                }
                            });
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Release Machine API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Release Machine JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Release Machine API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = throwable.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + throwable.getClass().getSimpleName();
                    }
                    errorResult.put("error", errorMessage);
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신을 배포합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return 배포 결과
     */
    public Mono<Map<String, Object>> deployMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-deploy";
        
        System.out.println("Deploy Machine - URL: " + url);
        System.out.println("Deploy Machine - systemId: " + systemId);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Deploy Machine API Error Response: " + errorBody);
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    return Mono.error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException(errorBody));
                                }
                            });
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Deploy Machine API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Deploy Machine JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Deploy Machine API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = throwable.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + throwable.getClass().getSimpleName();
                    }
                    errorResult.put("error", errorMessage);
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
     * MAAS 서버에서 모든 Fabric 목록을 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return Fabric 목록이 담긴 Map
     */
    public Mono<Map<String, Object>> getAllFabrics(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/fabrics/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("MAAS Fabric API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        
                        // MAAS API는 직접 배열을 반환하거나 {"results": [...]} 형태로 반환할 수 있음
                        if (jsonNode.isArray()) {
                            // 직접 배열 형태인 경우
                            System.out.println("Direct array response found: " + jsonNode.size() + " fabrics");
                            result.put("results", jsonNode);
                            result.put("count", jsonNode.size());
                        } else if (jsonNode.has("results")) {
                            // {"results": [...]} 형태인 경우
                            JsonNode results = jsonNode.get("results");
                            System.out.println("Results field found: " + results.size() + " fabrics");
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
                        
                        System.out.println("Final fabric result: " + result);
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
     * 특정 Fabric의 VLAN 목록을 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param fabricId Fabric ID
     * @return VLAN 목록이 담긴 Map
     */
    public Mono<Map<String, Object>> getFabricVlans(String maasUrl, String apiKey, String fabricId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/fabrics/" + fabricId + "/vlans/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("MAAS Fabric VLANs API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        
                        // MAAS API는 직접 배열을 반환하거나 {"results": [...]} 형태로 반환할 수 있음
                        if (jsonNode.isArray()) {
                            // 직접 배열 형태인 경우
                            System.out.println("Direct array response found: " + jsonNode.size() + " vlans");
                            result.put("results", jsonNode);
                            result.put("count", jsonNode.size());
                        } else if (jsonNode.has("results")) {
                            // {"results": [...]} 형태인 경우
                            JsonNode results = jsonNode.get("results");
                            System.out.println("Results field found: " + results.size() + " vlans");
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
                        
                        System.out.println("Final fabric vlans result: " + result);
                        return result;
                    } catch (Exception e) {
                        System.out.println("JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Fabric VLANs API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 모든 Subnet 목록을 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return Subnet 목록이 담긴 Map
     */
    public Mono<Map<String, Object>> getAllSubnets(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/subnets/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("MAAS Subnet API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        
                        // MAAS API는 직접 배열을 반환하거나 {"results": [...]} 형태로 반환할 수 있음
                        if (jsonNode.isArray()) {
                            // 직접 배열 형태인 경우
                            System.out.println("Direct array response found: " + jsonNode.size() + " subnets");
                            result.put("results", jsonNode);
                            result.put("count", jsonNode.size());
                        } else if (jsonNode.has("results")) {
                            // {"results": [...]} 형태인 경우
                            JsonNode results = jsonNode.get("results");
                            System.out.println("Results field found: " + results.size() + " subnets");
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
                        
                        System.out.println("Final subnet result: " + result);
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
     * 인터페이스의 VLAN을 업데이트합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param interfaceId 인터페이스 ID
     * @param vlanId VLAN ID
     * @return 업데이트 결과
     */
    public Mono<Map<String, Object>> updateInterfaceVlan(String maasUrl, String apiKey, 
            String systemId, String interfaceId, String vlanId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/nodes/" + systemId + "/interfaces/" + interfaceId + "/";
        
        // Form-data로 전송
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("vlan", vlanId);
        
        return webClient.put()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Update Interface VLAN API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Update Interface VLAN JSON parsing error: " + e.getMessage());
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
     * 인터페이스에 IP 주소를 링크합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param interfaceId 인터페이스 ID
     * @param ipAddress IP 주소 (null이면 AUTO mode로 설정)
     * @param subnetId Subnet ID
     * @return 링크 결과
     */
    public Mono<Map<String, Object>> linkSubnetToInterface(String maasUrl, String apiKey,
            String systemId, String interfaceId, String ipAddress, String subnetId) {
        // ipAddress가 null이거나 비어있으면 AUTO mode, 아니면 STATIC mode
        String mode = (ipAddress == null || ipAddress.isEmpty() || ipAddress.trim().isEmpty()) ? "AUTO" : "STATIC";
        System.out.println("Link Subnet - ipAddress: " + (ipAddress != null ? ipAddress : "null") + ", mode: " + mode);
        return linkSubnetToInterface(maasUrl, apiKey, systemId, interfaceId, ipAddress, subnetId, mode);
    }
    
    /**
     * 인터페이스에 IP 주소를 링크합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param interfaceId 인터페이스 ID
     * @param ipAddress IP 주소 (null이면 포함하지 않음)
     * @param subnetId Subnet ID
     * @param mode 링크 모드 (STATIC 또는 AUTO)
     * @return 링크 결과
     */
    public Mono<Map<String, Object>> linkSubnetToInterface(String maasUrl, String apiKey,
            String systemId, String interfaceId, String ipAddress, String subnetId, String mode) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/nodes/" + systemId + "/interfaces/" + interfaceId + "/op-link_subnet";
        
        System.out.println("Link Subnet to Interface - URL: " + url);
        System.out.println("Link Subnet to Interface - systemId: " + systemId + ", interfaceId: " + interfaceId + ", ipAddress: " + (ipAddress != null ? ipAddress : "null") + ", subnetId: " + subnetId + ", mode: " + mode);
        
        // Form-data로 전송
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // ipAddress가 null이거나 비어있으면 포함하지 않음
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            formData.add("ip_address", ipAddress);
        }
        formData.add("subnet", subnetId);
        formData.add("mode", mode);
        formData.add("default_gateway", "");
        formData.add("force", "");
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Link Subnet API Error Response: " + errorBody);
                                try {
                                    // JSON 응답인 경우 파싱 시도
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    return Mono.error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    // JSON이 아닌 경우 원본 메시지 사용
                                    return Mono.error(new RuntimeException(errorBody));
                                }
                            });
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Link Subnet API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Link Subnet JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Link Subnet API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    // 에러 메시지 추출 (RuntimeException으로 래핑된 경우 메시지 사용)
                    String errorMessage = throwable.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + throwable.getClass().getSimpleName();
                    }
                    errorResult.put("error", errorMessage);
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * 인터페이스에서 IP 주소 링크를 삭제합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param interfaceId 인터페이스 ID
     * @param linkId 링크 ID
     * @return 언링크 결과
     */
    public Mono<Map<String, Object>> unlinkSubnetFromInterface(String maasUrl, String apiKey,
            String systemId, String interfaceId, String linkId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/nodes/" + systemId + "/interfaces/" + interfaceId + "/op-unlink_subnet";
        
        System.out.println("Unlink Subnet from Interface - URL: " + url);
        System.out.println("Unlink Subnet from Interface - systemId: " + systemId + ", interfaceId: " + interfaceId + ", linkId: " + linkId);
        
        // Form-data로 전송
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", linkId);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Unlink Subnet API Error Response: " + errorBody);
                                try {
                                    // JSON 응답인 경우 파싱 시도
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    return Mono.error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    // JSON이 아닌 경우 원본 메시지 사용
                                    return Mono.error(new RuntimeException(errorBody));
                                }
                            });
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Unlink Subnet API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Unlink Subnet JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Unlink Subnet API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    // 에러 메시지 추출 (RuntimeException으로 래핑된 경우 메시지 사용)
                    String errorMessage = throwable.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + throwable.getClass().getSimpleName();
                    }
                    errorResult.put("error", errorMessage);
                    return Mono.just(errorResult);
                });
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
                            if (status == 4 || status == 10) { // Ready (Commissioned) or Allocated
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
    
    /**
     * MAAS 서버에서 특정 머신의 block devices 정보를 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return block devices 정보가 담긴 Map
     */
    public Mono<Map<String, Object>> getMachineBlockDevices(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/block_devices/";
        
        System.out.println("Get Machine Block Devices - URL: " + url);
        System.out.println("Get Machine Block Devices - systemId: " + systemId);
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Block Devices API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        
                        // MAAS API는 직접 배열을 반환하거나 {"results": [...]} 형태로 반환할 수 있음
                        if (jsonNode.isArray()) {
                            // 직접 배열 형태인 경우 - JsonNode를 List로 변환
                            System.out.println("Direct array response found: " + jsonNode.size() + " block devices");
                            List<Map<String, Object>> blockDevicesList = new ArrayList<>();
                            for (JsonNode deviceNode : jsonNode) {
                                blockDevicesList.add(objectMapper.convertValue(deviceNode, Map.class));
                            }
                            result.put("results", blockDevicesList);
                            result.put("count", blockDevicesList.size());
                        } else if (jsonNode.has("results")) {
                            // {"results": [...]} 형태인 경우
                            JsonNode results = jsonNode.get("results");
                            System.out.println("Results field found: " + results.size() + " block devices");
                            List<Map<String, Object>> blockDevicesList = new ArrayList<>();
                            if (results.isArray()) {
                                for (JsonNode deviceNode : results) {
                                    blockDevicesList.add(objectMapper.convertValue(deviceNode, Map.class));
                                }
                            }
                            result.put("results", blockDevicesList);
                            if (jsonNode.has("count")) {
                                result.put("count", jsonNode.get("count").asInt());
                            } else {
                                result.put("count", blockDevicesList.size());
                            }
                        } else {
                            System.out.println("Unexpected response format");
                            result.put("results", new ArrayList<>());
                            result.put("count", 0);
                        }
                        
                        System.out.println("Final block devices result: " + result);
                        return result;
                    } catch (Exception e) {
                        System.out.println("JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.out.println("Block Devices API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신을 삭제합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return 삭제 결과
     */
    public Mono<Map<String, Object>> deleteMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/";
        
        System.out.println("Delete Machine - URL: " + url);
        System.out.println("Delete Machine - systemId: " + systemId);
        
        return webClient.delete()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(errorBody -> {
                                System.out.println("Delete Machine API Error Response: " + errorBody);
                                try {
                                    if (errorBody != null && !errorBody.isEmpty()) {
                                        JsonNode jsonNode = objectMapper.readTree(errorBody);
                                        String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                        return Mono.error(new RuntimeException(errorMessage));
                                    } else {
                                        return Mono.error(new RuntimeException("Delete failed with status: " + response.statusCode()));
                                    }
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException(errorBody != null && !errorBody.isEmpty() ? errorBody : "Delete failed"));
                                }
                            });
                })
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    System.out.println("Delete Machine API Response: " + (responseBody != null && !responseBody.isEmpty() ? responseBody.substring(0, Math.min(500, responseBody.length())) : "empty or no content"));
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    if (responseBody != null && !responseBody.isEmpty()) {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);
                            result.put("data", jsonNode);
                        } catch (Exception e) {
                            result.put("message", responseBody);
                        }
                    } else {
                        result.put("message", "Machine deleted successfully");
                    }
                    return result;
                })
                .onErrorResume(throwable -> {
                    System.out.println("Delete Machine API Error: " + throwable.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = throwable.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + throwable.getClass().getSimpleName();
                    }
                    errorResult.put("error", errorMessage);
                    return Mono.just(errorResult);
                });
    }
}
