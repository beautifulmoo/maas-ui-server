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
        
        System.out.println("Abort - URL: " + url);
        System.out.println("Abort - systemId: " + systemId);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Abort API Error Response: " + errorBody);
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
                .timeout(Duration.ofSeconds(60)) // 타임아웃을 60초로 증가
                .map(responseBody -> {
                    try {
                        System.out.println("Abort API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Abort JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("Abort API Error: " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + e.getClass().getSimpleName();
                    }
                    // 타임아웃 에러인 경우 더 명확한 메시지
                    if (errorMessage.contains("timeout") || errorMessage.contains("30000") || errorMessage.contains("60000")) {
                        errorMessage = "Abort operation timed out. The operation may still be in progress.";
                    }
                    errorResult.put("error", "Failed to abort: " + errorMessage);
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신의 전원을 켭니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return Power on 결과
     */
    public Mono<Map<String, Object>> powerOnMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-power_on";
        
        System.out.println("Power On - URL: " + url);
        System.out.println("Power On - systemId: " + systemId);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Power On API Error Response: " + errorBody);
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
                        System.out.println("Power On API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Power On JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("Power On API Error: " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + e.getClass().getSimpleName();
                    }
                    errorResult.put("error", "Failed to power on: " + errorMessage);
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신의 전원을 끕니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return Power off 결과
     */
    public Mono<Map<String, Object>> powerOffMachine(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-power_off";
        
        System.out.println("Power Off - URL: " + url);
        System.out.println("Power Off - systemId: " + systemId);
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Power Off API Error Response: " + errorBody);
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
                        System.out.println("Power Off API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Power Off JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("Power Off API Error: " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + e.getClass().getSimpleName();
                    }
                    errorResult.put("error", "Failed to power off: " + errorMessage);
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * MAAS 서버에서 머신의 전원 상태를 조회합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return Power state 조회 결과 (state: on, off, unknown 등)
     */
    public Mono<Map<String, Object>> queryPowerState(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-query_power_state";
        
        System.out.println("Query Power State - URL: " + url);
        System.out.println("Query Power State - systemId: " + systemId);
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.out.println("Query Power State API Error Response: " + errorBody);
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
                        System.out.println("Query Power State API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Query Power State JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "Failed to parse response: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("Query Power State API Error: " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "API call failed: " + e.getClass().getSimpleName();
                    }
                    errorResult.put("error", "Failed to query power state: " + errorMessage);
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
     * @param os OS 이름 (선택)
     * @param release OS release (선택)
     * @param arch Architecture (선택)
     * @return 배포 결과
     */
    public Mono<Map<String, Object>> deployMachine(String maasUrl, String apiKey, String systemId, 
            String os, String release, String arch) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-deploy";
        
        System.out.println("Deploy Machine - URL: " + url);
        System.out.println("Deploy Machine - systemId: " + systemId);
        if (os != null) {
            System.out.println("Deploy Machine - OS: " + os + ", Release: " + release + ", Arch: " + arch);
        }
        
        // Form data 생성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        
        // distro_series 형식: ubuntu/noble, ubuntu/jammy 등
        if (os != null && !os.trim().isEmpty() && release != null && !release.trim().isEmpty()) {
            String distroSeries = os.trim() + "/" + release.trim();
            formData.add("distro_series", distroSeries);
            System.out.println("Deploy Machine - distro_series: " + distroSeries);
        }
        
        if (arch != null && !arch.trim().isEmpty()) {
            formData.add("architecture", arch.trim());
        }
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(formData.isEmpty() ? BodyInserters.empty() : BodyInserters.fromFormData(formData))
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
     * @param powerDriver Power Driver (IPMI)
     * @param powerBootType Power Boot Type (IPMI)
     * @param powerIpAddress IP Address (IPMI)
     * @param powerUser Power User (IPMI)
     * @param powerPassword Power Password (IPMI)
     * @param powerKgBmcKey K_g BMC key (IPMI)
     * @param cipherSuiteId Cipher Suite ID (IPMI)
     * @param privilegeLevel Privilege Level (IPMI)
     * @param workaroundFlags Workaround Flags (IPMI)
     * @param powerMac Power MAC (IPMI)
     * @return 머신 추가 결과
     */
    public Mono<Map<String, Object>> addMachine(String maasUrl, String apiKey, 
            String hostname, String architecture, String macAddresses, 
            String powerType, String commission, String description,
            String powerDriver, String powerBootType, String powerIpAddress,
            String powerUser, String powerPassword, String powerKgBmcKey,
            String cipherSuiteId, String privilegeLevel, String workaroundFlags,
            String powerMac) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/";
        
        MultiValueMap<String, String> formData = createFormData(hostname, architecture, macAddresses, 
                powerType, commission, description,
                powerDriver, powerBootType, powerIpAddress, powerUser, powerPassword,
                powerKgBmcKey, cipherSuiteId, privilegeLevel, workaroundFlags, powerMac);
        
        // Log form data for debugging - detailed logging
        System.err.println("========== Add Machine Request ==========");
        System.err.println("URL: " + url);
        System.err.println("Form Data Keys: " + formData.keySet());
        for (String key : formData.keySet()) {
            System.err.println("  " + key + " = " + formData.getFirst(key));
        }
        System.err.println("==========================================");
        
        return webClient.post()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    System.err.println("========== Add Machine API Error ==========");
                    System.err.println("Error Status: " + response.statusCode());
                    System.err.println("Error Headers: " + response.headers().asHttpHeaders());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Error Response Body (length: " + errorBody.length() + "):");
                                System.err.println(errorBody);
                                // Print each character if it's short
                                if (errorBody.length() < 200) {
                                    byte[] bytes = errorBody.getBytes();
                                    StringBuilder hex = new StringBuilder();
                                    for (byte b : bytes) {
                                        hex.append(String.format("%02x ", b));
                                    }
                                    System.err.println("Error Body (hex): " + hex.toString());
                                }
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    System.err.println("Parsed Error Message: " + errorMessage);
                                    System.err.println("==========================================");
                                    return Mono.<RuntimeException>error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    System.err.println("Error parsing JSON: " + e.getMessage());
                                    System.err.println("Raw Error Body: " + errorBody);
                                    System.err.println("==========================================");
                                    return Mono.<RuntimeException>error(new RuntimeException(errorBody));
                                }
                            })
                            .cast(Throwable.class);
                })
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
                    System.err.println("========== Add Machine Error (onErrorResume) ==========");
                    System.err.println("Error Message: " + throwable.getMessage());
                    System.err.println("Error Class: " + throwable.getClass().getName());
                    throwable.printStackTrace();
                    System.err.println("========================================================");
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
    private MultiValueMap<String, String> createFormData(String hostname, String architecture, 
            String macAddresses, String powerType, String commission, String description,
            String powerDriver, String powerBootType, String powerIpAddress,
            String powerUser, String powerPassword, String powerKgBmcKey,
            String cipherSuiteId, String privilegeLevel, String workaroundFlags,
            String powerMac) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        
        if (hostname != null && !hostname.trim().isEmpty()) {
            formData.add("hostname", hostname.trim());
        }
        formData.add("architecture", architecture);
        formData.add("mac_addresses", macAddresses);
        formData.add("power_type", powerType);
        formData.add("commission", commission);
        if (description != null && !description.trim().isEmpty()) {
            formData.add("description", description.trim());
        }
        
        // Add IPMI parameters only if power type is "ipmi"
        if ("ipmi".equals(powerType)) {
            // Default values are always included (powerDriver, powerBootType, cipherSuiteId, privilegeLevel, workaroundFlags)
            if (powerDriver != null && !powerDriver.trim().isEmpty()) {
                formData.add("power_parameters_power_driver", powerDriver.trim());
            }
            if (powerBootType != null && !powerBootType.trim().isEmpty()) {
                formData.add("power_parameters_power_boot_type", powerBootType.trim());
            }
            // Optional fields - only include if not empty (powerIpAddress, powerUser, powerPassword, powerKgBmcKey, powerMac)
            if (powerIpAddress != null && !powerIpAddress.trim().isEmpty()) {
                formData.add("power_parameters_power_address", powerIpAddress.trim());
            }
            if (powerUser != null && !powerUser.trim().isEmpty()) {
                formData.add("power_parameters_power_user", powerUser.trim());
            }
            if (powerPassword != null && !powerPassword.trim().isEmpty()) {
                formData.add("power_parameters_power_pass", powerPassword.trim());
            }
            // K_g BMC key - only include if not empty
            if (powerKgBmcKey != null && !powerKgBmcKey.trim().isEmpty()) {
                formData.add("power_parameters_k_g", powerKgBmcKey.trim());
            }
            // Default value exists, but only include if not empty
            if (cipherSuiteId != null && !cipherSuiteId.trim().isEmpty()) {
                formData.add("power_parameters_cipher_suite_id", cipherSuiteId.trim());
            }
            if (privilegeLevel != null && !privilegeLevel.trim().isEmpty()) {
                formData.add("power_parameters_privilege_level", privilegeLevel.trim());
            }
            if (workaroundFlags != null && !workaroundFlags.trim().isEmpty()) {
                formData.add("power_parameters_workaround_flags", workaroundFlags.trim());
            }
            // Power MAC - only include if not empty
            if (powerMac != null && !powerMac.trim().isEmpty()) {
                formData.add("power_parameters_mac_address", powerMac.trim());
            }
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
        // vlanId가 빈 문자열이거나 null이면 vlan=""로 설정하여 VLAN 삭제
        if (vlanId == null || vlanId.trim().isEmpty()) {
            formData.add("vlan", "");
        } else {
            formData.add("vlan", vlanId);
        }
        
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
     * MAAS 서버에서 특정 머신의 power parameters 정보를 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @return power parameters 정보가 담긴 Map
     */
    public Mono<Map<String, Object>> getMachinePowerParameters(String maasUrl, String apiKey, String systemId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/op-power_parameters";
        
        System.out.println("Get Machine Power Parameters - URL: " + url);
        System.out.println("Get Machine Power Parameters - systemId: " + systemId);
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Power Parameters API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = objectMapper.convertValue(jsonNode, Map.class);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Power Parameters JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("Power Parameters API call failed: " + e.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Failed to fetch power parameters: " + e.getMessage());
                    return Mono.just(errorResult);
                });
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
    
    /**
     * MAAS 서버에서 모든 이벤트를 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return 이벤트 목록
     */
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> getEvents(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/events/op-query";
        
        System.out.println("Get Events - URL: " + url);
        System.out.println("Get Events - Auth Header: " + authHeader);
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    System.out.println("Get Events - Response Body Length: " + (responseBody != null ? responseBody.length() : 0));
                    System.out.println("Get Events - Response Body (first 500 chars): " + 
                        (responseBody != null && responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                    
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        System.out.println("Get Events - JSON Node Type: " + jsonNode.getNodeType());
                        
                        // 응답 형식: {count: 100, events: [...]}
                        if (jsonNode.has("events") && jsonNode.get("events").isArray()) {
                            JsonNode eventsArray = jsonNode.get("events");
                            System.out.println("Get Events - Events Array Size: " + eventsArray.size());
                            
                            List<Map<String, Object>> events = new ArrayList<>();
                            for (JsonNode eventNode : eventsArray) {
                                Map<String, Object> event = objectMapper.convertValue(eventNode, Map.class);
                                events.add((Map<String, Object>) event);
                            }
                            System.out.println("Get Events - Parsed Events Count: " + events.size());
                            if (events.size() > 0) {
                                System.out.println("Get Events - First Event: " + events.get(0));
                            }
                            return events;
                        } else if (jsonNode.isArray()) {
                            // 배열로 직접 오는 경우도 처리 (이전 형식 호환)
                            System.out.println("Get Events - Response is direct array, Size: " + jsonNode.size());
                            List<Map<String, Object>> events = new ArrayList<>();
                            for (JsonNode eventNode : jsonNode) {
                                Map<String, Object> event = objectMapper.convertValue(eventNode, Map.class);
                                events.add((Map<String, Object>) event);
                            }
                            return events;
                        } else {
                            System.out.println("Get Events - Response does not have 'events' field or is not an array");
                            System.out.println("Get Events - Available fields: " + jsonNode.fieldNames());
                        }
                        return new ArrayList<Map<String, Object>>();
                    } catch (Exception e) {
                        System.err.println("Error parsing events response: " + e.getMessage());
                        e.printStackTrace();
                        System.err.println("Response body that caused error: " + responseBody);
                        return new ArrayList<Map<String, Object>>();
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error fetching events: " + e.getMessage());
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        System.err.println("Error cause: " + e.getCause().getMessage());
                    }
                    return Mono.just(new ArrayList<Map<String, Object>>());
                });
    }
    
    /**
     * MAAS 서버에서 머신의 Power 파라미터를 업데이트합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param systemId 머신의 시스템 ID
     * @param powerType 전원 타입
     * @param powerDriver Power Driver (IPMI)
     * @param powerBootType Power Boot Type (IPMI)
     * @param powerIpAddress IP Address (IPMI)
     * @param powerUser Power User (IPMI)
     * @param powerPassword Power Password (IPMI)
     * @param powerKgBmcKey K_g BMC key (IPMI)
     * @param cipherSuiteId Cipher Suite ID (IPMI)
     * @param privilegeLevel Privilege Level (IPMI)
     * @param workaroundFlags Workaround Flags (IPMI)
     * @param powerMac Power MAC (IPMI)
     * @return 업데이트 결과
     */
    public Mono<Map<String, Object>> updateMachinePowerParameters(String maasUrl, String apiKey, 
            String systemId, String powerType,
            String powerDriver, String powerBootType, String powerIpAddress,
            String powerUser, String powerPassword, String powerKgBmcKey,
            String cipherSuiteId, String privilegeLevel, String workaroundFlags,
            String powerMac) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/machines/" + systemId + "/";
        
        MultiValueMap<String, String> formData = createPowerParametersFormData(powerType,
                powerDriver, powerBootType, powerIpAddress, powerUser, powerPassword,
                powerKgBmcKey, cipherSuiteId, privilegeLevel, workaroundFlags, powerMac);
        
        // Log form data for debugging
        System.out.println("========== Update Machine Power Parameters Request ==========");
        System.out.println("URL: " + url);
        System.out.println("System ID: " + systemId);
        System.out.println("Form Data Keys: " + formData.keySet());
        for (String key : formData.keySet()) {
            System.out.println("  " + key + " = " + formData.getFirst(key));
        }
        System.out.println("==============================================================");
        
        return webClient.put()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    System.err.println("========== Update Machine Power Parameters API Error ==========");
                    System.err.println("Error Status: " + response.statusCode());
                    System.err.println("Error Headers: " + response.headers().asHttpHeaders());
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Error Response Body (length: " + errorBody.length() + "):");
                                System.err.println(errorBody);
                                if (errorBody.length() < 200) {
                                    byte[] bytes = errorBody.getBytes();
                                    StringBuilder hex = new StringBuilder();
                                    for (byte b : bytes) {
                                        hex.append(String.format("%02x ", b));
                                    }
                                    System.err.println("Error Body (hex): " + hex.toString());
                                }
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(errorBody);
                                    String errorMessage = jsonNode.has("error") ? jsonNode.get("error").asText() : errorBody;
                                    System.err.println("Parsed Error Message: " + errorMessage);
                                    System.err.println("==========================================");
                                    return Mono.<RuntimeException>error(new RuntimeException(errorMessage));
                                } catch (Exception e) {
                                    System.err.println("Error parsing JSON: " + e.getMessage());
                                    System.err.println("Raw Error Body: " + errorBody);
                                    System.err.println("==========================================");
                                    return Mono.<RuntimeException>error(new RuntimeException(errorBody));
                                }
                            })
                            .cast(Throwable.class);
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        System.out.println("Update Machine Power Parameters API Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", jsonNode);
                        return result;
                    } catch (Exception e) {
                        System.out.println("Update Machine Power Parameters JSON parsing error: " + e.getMessage());
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", "JSON parsing error: " + e.getMessage());
                        return errorResult;
                    }
                })
                .onErrorResume(throwable -> {
                    System.err.println("========== Update Machine Power Parameters Error (onErrorResume) ==========");
                    System.err.println("Error Message: " + throwable.getMessage());
                    System.err.println("Error Class: " + throwable.getClass().getName());
                    throwable.printStackTrace();
                    System.err.println("========================================================");
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("error", "API call failed: " + throwable.getMessage());
                    return Mono.just(errorResult);
                });
    }
    
    /**
     * Power 파라미터용 폼 데이터를 생성합니다.
     */
    private MultiValueMap<String, String> createPowerParametersFormData(String powerType,
            String powerDriver, String powerBootType, String powerIpAddress,
            String powerUser, String powerPassword, String powerKgBmcKey,
            String cipherSuiteId, String privilegeLevel, String workaroundFlags,
            String powerMac) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        
        // Always include power_type
        formData.add("power_type", powerType);
        
        // Add IPMI parameters only if power type is "ipmi"
        if ("ipmi".equals(powerType)) {
            if (powerDriver != null && !powerDriver.trim().isEmpty()) {
                formData.add("power_parameters_power_driver", powerDriver.trim());
            }
            if (powerBootType != null && !powerBootType.trim().isEmpty()) {
                formData.add("power_parameters_power_boot_type", powerBootType.trim());
            }
            if (powerIpAddress != null && !powerIpAddress.trim().isEmpty()) {
                formData.add("power_parameters_power_address", powerIpAddress.trim());
            }
            if (powerUser != null && !powerUser.trim().isEmpty()) {
                formData.add("power_parameters_power_user", powerUser.trim());
            }
            if (powerPassword != null && !powerPassword.trim().isEmpty()) {
                formData.add("power_parameters_power_pass", powerPassword.trim());
            }
            if (powerKgBmcKey != null && !powerKgBmcKey.trim().isEmpty()) {
                formData.add("power_parameters_k_g", powerKgBmcKey.trim());
            }
            if (cipherSuiteId != null && !cipherSuiteId.trim().isEmpty()) {
                formData.add("power_parameters_cipher_suite_id", cipherSuiteId.trim());
            }
            if (privilegeLevel != null && !privilegeLevel.trim().isEmpty()) {
                formData.add("power_parameters_privilege_level", privilegeLevel.trim());
            }
            if (workaroundFlags != null && !workaroundFlags.trim().isEmpty()) {
                formData.add("power_parameters_workaround_flags", workaroundFlags.trim());
            }
            if (powerMac != null && !powerMac.trim().isEmpty()) {
                formData.add("power_parameters_mac_address", powerMac.trim());
            }
        }
        
        return formData;
    }
    
    /**
     * MAAS 서버에서 boot sources 목록을 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return boot sources 목록
     */
    public Mono<List<Map<String, Object>>> getBootSources(String maasUrl, String apiKey) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/boot-sources/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        List<Map<String, Object>> bootSources = new ArrayList<>();
                        
                        if (jsonNode.isArray()) {
                            for (JsonNode source : jsonNode) {
                                Map<String, Object> sourceMap = new HashMap<>();
                                if (source.has("id")) {
                                    sourceMap.put("id", source.get("id").asInt());
                                }
                                if (source.has("url")) {
                                    sourceMap.put("url", source.get("url").asText());
                                }
                                if (source.has("keyring_filename")) {
                                    sourceMap.put("keyring_filename", source.get("keyring_filename").asText());
                                }
                                if (source.has("keyring_data")) {
                                    sourceMap.put("keyring_data", source.get("keyring_data").asText());
                                }
                                bootSources.add(sourceMap);
                            }
                        }
                        
                        return bootSources;
                    } catch (Exception e) {
                        System.err.println("Error parsing boot sources: " + e.getMessage());
                        e.printStackTrace();
                        return new ArrayList<Map<String, Object>>();
                    }
                })
                .onErrorResume(throwable -> {
                    System.err.println("Error fetching boot sources: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return Mono.just(new ArrayList<Map<String, Object>>());
                });
    }
    
    /**
     * 특정 boot source의 selections 목록을 가져옵니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @param bootSourceId boot source ID
     * @return selections 목록
     */
    public Mono<List<Map<String, Object>>> getBootSourceSelections(String maasUrl, String apiKey, int bootSourceId) {
        String authHeader = authService.generateAuthHeader(apiKey);
        String url = maasUrl + "/MAAS/api/2.0/boot-sources/" + bootSourceId + "/selections/";
        
        return webClient.get()
                .uri(url)
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(responseBody);
                        List<Map<String, Object>> selections = new ArrayList<>();
                        
                        if (jsonNode.isArray()) {
                            for (JsonNode selection : jsonNode) {
                                Map<String, Object> selectionMap = new HashMap<>();
                                if (selection.has("os")) {
                                    selectionMap.put("os", selection.get("os").asText());
                                }
                                if (selection.has("release")) {
                                    selectionMap.put("release", selection.get("release").asText());
                                }
                                if (selection.has("arches")) {
                                    List<String> arches = new ArrayList<>();
                                    JsonNode archesNode = selection.get("arches");
                                    if (archesNode.isArray()) {
                                        for (JsonNode arch : archesNode) {
                                            arches.add(arch.asText());
                                        }
                                    }
                                    selectionMap.put("arches", arches);
                                }
                                if (selection.has("subarches")) {
                                    List<String> subarches = new ArrayList<>();
                                    JsonNode subarchesNode = selection.get("subarches");
                                    if (subarchesNode.isArray()) {
                                        for (JsonNode subarch : subarchesNode) {
                                            subarches.add(subarch.asText());
                                        }
                                    }
                                    selectionMap.put("subarches", subarches);
                                }
                                if (selection.has("labels")) {
                                    List<String> labels = new ArrayList<>();
                                    JsonNode labelsNode = selection.get("labels");
                                    if (labelsNode.isArray()) {
                                        for (JsonNode label : labelsNode) {
                                            labels.add(label.asText());
                                        }
                                    }
                                    selectionMap.put("labels", labels);
                                }
                                if (selection.has("id")) {
                                    selectionMap.put("id", selection.get("id").asInt());
                                }
                                if (selection.has("boot_source_id")) {
                                    selectionMap.put("boot_source_id", selection.get("boot_source_id").asInt());
                                }
                                selections.add(selectionMap);
                            }
                        }
                        
                        return selections;
                    } catch (Exception e) {
                        System.err.println("Error parsing boot source selections: " + e.getMessage());
                        e.printStackTrace();
                        return new ArrayList<Map<String, Object>>();
                    }
                })
                .onErrorResume(throwable -> {
                    System.err.println("Error fetching boot source selections: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return Mono.just(new ArrayList<Map<String, Object>>());
                });
    }
    
    /**
     * 모든 deployable OS 목록을 가져옵니다.
     * 모든 boot sources를 순회하며 각 source의 selections를 수집합니다.
     * 
     * @param maasUrl MAAS 서버 URL
     * @param apiKey MAAS API 키
     * @return deployable OS 목록
     */
    public Mono<List<Map<String, Object>>> getAllDeployableOS(String maasUrl, String apiKey) {
        return getBootSources(maasUrl, apiKey)
                .flatMap(bootSources -> {
                    if (bootSources.isEmpty()) {
                        return Mono.just(new ArrayList<Map<String, Object>>());
                    }
                    
                    // 모든 boot source의 selections를 순차적으로 가져오기
                    List<Map<String, Object>> allSelections = new ArrayList<>();
                    Mono<List<Map<String, Object>>> resultMono = Mono.just(allSelections);
                    
                    for (Map<String, Object> source : bootSources) {
                        Integer id = (Integer) source.get("id");
                        if (id != null) {
                            resultMono = resultMono.flatMap(selections -> {
                                return getBootSourceSelections(maasUrl, apiKey, id)
                                        .map(newSelections -> {
                                            selections.addAll(newSelections);
                                            return selections;
                                        });
                            });
                        }
                    }
                    
                    return resultMono;
                })
                .onErrorResume(throwable -> {
                    System.err.println("Error fetching all deployable OS: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return Mono.just(new ArrayList<Map<String, Object>>());
                });
    }
}
