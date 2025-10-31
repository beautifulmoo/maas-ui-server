package com.maas.ui.controller;

import com.maas.ui.service.MaasApiService;
import com.maas.ui.service.MaasAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // CORS 허용
public class MaasController {
    
    @Autowired
    private MaasApiService maasApiService;
    
    @Autowired
    private MaasAuthService authService;
    
    /**
     * 머신 통계 정보를 가져옵니다.
     */
    @GetMapping("/dashboard/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardStats(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getAllMachines(maasUrl, apiKey)
                .map(machinesData -> {
                    Map<String, Integer> stats = maasApiService.calculateMachineStats(machinesData);
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("totalMachines", stats.get("total"));
                    result.put("commissionedMachines", stats.get("commissioned"));
                    result.put("deployedMachines", stats.get("deployed"));
                    return ResponseEntity.ok(result);
                })
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch machine statistics")));
    }
    
    /**
     * 모든 머신 정보를 가져옵니다.
     */
    @GetMapping("/machines")
    public Mono<ResponseEntity<Map<String, Object>>> getAllMachines(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getAllMachines(maasUrl, apiKey)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch machines")));
    }
    
    /**
     * MAAS 서버 연결을 테스트합니다.
     */
    @GetMapping("/test-connection")
    public Mono<ResponseEntity<Map<String, Object>>> testConnection(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.testConnection(maasUrl, apiKey)
                .map(result -> {
                    boolean success = (Boolean) result.get("success");
                    if (success) {
                        return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Connection successful! MAAS server is reachable."
                        ));
                    } else {
                        return ResponseEntity.status(400).body(Map.of(
                            "success", false,
                            "error", result.get("error")
                        ));
                    }
                });
    }
    
    /**
     * 새 머신을 추가합니다.
     */
    @PostMapping("/machines")
    public Mono<ResponseEntity<Map<String, Object>>> addMachine(
            @RequestParam String maasUrl,
            @RequestParam String apiKey,
            @RequestParam(required = false) String hostname,
            @RequestParam(defaultValue = "amd64") String architecture,
            @RequestParam String macAddresses,
            @RequestParam(defaultValue = "manual") String powerType,
            @RequestParam(defaultValue = "false") String commission,
            @RequestParam(required = false) String description) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.addMachine(maasUrl, apiKey, hostname, architecture, 
                macAddresses, powerType, commission, description)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to add machine")));
    }
    
    /**
     * 머신을 커미셔닝합니다.
     */
    @PostMapping("/machines/{systemId}/commission")
    public Mono<ResponseEntity<Map<String, Object>>> commissionMachine(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey,
            @RequestParam(defaultValue = "1") String skipBmcConfig) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.commissionMachine(maasUrl, apiKey, systemId, skipBmcConfig)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to commission machine")));
    }
    
    /**
     * 머신의 커미셔닝을 중단합니다.
     */
    @PostMapping("/machines/{systemId}/abort")
    public Mono<ResponseEntity<Map<String, Object>>> abortCommissioning(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.abortCommissioning(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to abort commissioning")));
    }
    
    /**
     * 헬스 체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "MAAS UI Backend"));
    }
}
