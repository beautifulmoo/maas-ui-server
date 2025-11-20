package com.maas.ui.controller;

import com.maas.ui.service.MaasApiService;
import com.maas.ui.service.MaasAuthService;
import com.maas.ui.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // CORS 허용
public class MaasController {
    
    @Autowired
    private MaasApiService maasApiService;
    
    @Autowired
    private MaasAuthService authService;
    
    @Autowired
    private SettingsService settingsService;
    
    /**
     * 설정을 가져옵니다.
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        try {
            Map<String, Object> settings = settingsService.loadSettings();
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to load settings: " + e.getMessage()));
        }
    }
    
    /**
     * 설정을 저장합니다.
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> settings) {
        try {
            boolean success = settingsService.saveSettings(settings);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Settings saved successfully"));
            } else {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "error", "Failed to save settings"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Failed to save settings: " + e.getMessage()));
        }
    }
    
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
     * 개별 머신 정보를 가져옵니다.
     */
    @GetMapping("/machines/{systemId}")
    public Mono<ResponseEntity<Map<String, Object>>> getMachine(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getMachine(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch machine")));
    }
    
    /**
     * 특정 머신의 block devices 정보를 가져옵니다.
     */
    @GetMapping("/machines/{systemId}/block-devices")
    public Mono<ResponseEntity<Map<String, Object>>> getMachineBlockDevices(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getMachineBlockDevices(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch machine block devices")));
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
     * 머신을 배포합니다.
     */
    @PostMapping("/machines/{systemId}/release")
    public Mono<ResponseEntity<Map<String, Object>>> releaseMachine(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.releaseMachine(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to release machine")));
    }
    
    @PostMapping("/machines/{systemId}/deploy")
    public Mono<ResponseEntity<Map<String, Object>>> deployMachine(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.deployMachine(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to deploy machine")));
    }
    
    /**
     * 모든 Fabric 목록을 가져옵니다.
     */
    @GetMapping("/fabrics")
    public Mono<ResponseEntity<Map<String, Object>>> getAllFabrics(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getAllFabrics(maasUrl, apiKey)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch fabrics")));
    }
    
    /**
     * 특정 Fabric의 VLAN 목록을 가져옵니다.
     */
    @GetMapping("/fabrics/{fabricId}/vlans")
    public Mono<ResponseEntity<Map<String, Object>>> getFabricVlans(
            @PathVariable String fabricId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getFabricVlans(maasUrl, apiKey, fabricId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch fabric vlans")));
    }
    
    /**
     * 모든 Subnet 목록을 가져옵니다.
     */
    @GetMapping("/subnets")
    public Mono<ResponseEntity<Map<String, Object>>> getAllSubnets(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.getAllSubnets(maasUrl, apiKey)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch subnets")));
    }
    
    /**
     * 인터페이스의 VLAN을 업데이트합니다.
     */
    @PutMapping("/machines/{systemId}/interfaces/{interfaceId}/vlan")
    public Mono<ResponseEntity<Map<String, Object>>> updateInterfaceVlan(
            @PathVariable String systemId,
            @PathVariable String interfaceId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey,
            @RequestParam String vlanId) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.updateInterfaceVlan(maasUrl, apiKey, systemId, interfaceId, vlanId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to update interface VLAN")));
    }
    
    /**
     * 인터페이스에 IP 주소를 링크합니다.
     */
    @PostMapping("/machines/{systemId}/interfaces/{interfaceId}/link-subnet")
    public Mono<ResponseEntity<Map<String, Object>>> linkSubnetToInterface(
            @PathVariable String systemId,
            @PathVariable String interfaceId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey,
            @RequestParam(required = false) String ipAddress,
            @RequestParam String subnetId) {
        
        System.out.println("Controller - linkSubnetToInterface called: systemId=" + systemId + ", interfaceId=" + interfaceId + ", ipAddress=" + (ipAddress != null ? ipAddress : "null") + ", subnetId=" + subnetId);
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.linkSubnetToInterface(maasUrl, apiKey, systemId, interfaceId, ipAddress, subnetId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to link subnet to interface")));
    }
    
    /**
     * 인터페이스에서 IP 주소 링크를 삭제합니다.
     */
    @PostMapping("/machines/{systemId}/interfaces/{interfaceId}/unlink-subnet")
    public Mono<ResponseEntity<Map<String, Object>>> unlinkSubnetFromInterface(
            @PathVariable String systemId,
            @PathVariable String interfaceId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey,
            @RequestParam String linkId) {
        
        if (!authService.isValidApiKey(apiKey)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid API key format")));
        }
        
        return maasApiService.unlinkSubnetFromInterface(maasUrl, apiKey, systemId, interfaceId, linkId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to unlink subnet from interface")));
    }
    
    /**
     * 머신을 삭제합니다.
     */
    @DeleteMapping("/machines/{systemId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteMachine(
            @PathVariable String systemId,
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        return maasApiService.deleteMachine(maasUrl, apiKey, systemId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to delete machine")));
    }
    
    /**
     * MAAS 이벤트를 조회합니다.
     */
    @GetMapping("/events/op-query")
    public Mono<ResponseEntity<Map<String, Object>>> getEvents(
            @RequestParam String maasUrl,
            @RequestParam String apiKey) {
        return maasApiService.getEvents(maasUrl, apiKey)
                .map(events -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("results", events);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    System.err.println("Error fetching events: " + e.getMessage());
                    e.printStackTrace();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Failed to fetch events: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }
    
    /**
     * 헬스 체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "MAAS UI Backend"));
    }
}
