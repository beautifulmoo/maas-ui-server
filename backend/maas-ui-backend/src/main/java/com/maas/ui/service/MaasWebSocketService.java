package com.maas.ui.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MaasWebSocketService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    private WebSocketClient webSocketClient;
    private Session maasSession;
    private String csrfToken;
    private String sessionId;
    private Consumer<String> messageCallback;
    private boolean connected = false;
    private ScheduledExecutorService heartbeatScheduler;
    private long lastActivityTime = System.currentTimeMillis();
    private boolean shouldReconnect = true; // 재연결이 필요한지 여부

    // MAAS 서버 설정 (현재 하드코딩, 나중에 설정 파일로 이동)
    private static final String MAAS_HOST = "192.168.189.71:5240";
    private static final String MAAS_USERNAME = "admin";
    private static final String MAAS_PASSWORD = "admin";
    private static final String MAAS_LOGIN_URL = "http://" + MAAS_HOST + "/MAAS/accounts/login/";
    private static final String MAAS_WS_URL = "ws://" + MAAS_HOST + "/MAAS/ws";
    private static final long HEARTBEAT_INTERVAL = 30000; // 30초
    private static final long MAX_IDLE_TIME = 300000; // 5분

    @PostConstruct
    public void init() {
        try {
            // Jetty WebSocketClient 초기화
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            webSocketClient = new WebSocketClient(httpClient);
            webSocketClient.start();
            log.info("Jetty WebSocketClient 초기화 완료");
        } catch (Exception e) {
            log.error("WebSocketClient 초기화 실패", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            // Heartbeat 스케줄러 종료
            if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
                heartbeatScheduler.shutdown();
                try {
                    if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        heartbeatScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    heartbeatScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            if (maasSession != null && maasSession.isOpen()) {
                maasSession.close();
            }
            if (webSocketClient != null) {
                webSocketClient.stop();
            }
            log.info("WebSocket 연결 종료 및 정리 완료");
        } catch (Exception e) {
            log.error("정리 중 오류 발생", e);
        }
    }

    /**
     * MAAS 서버에 로그인하여 CSRF 토큰과 세션 ID 획득
     */
    private void performLogin() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = String.format("username=%s&password=%s", MAAS_USERNAME, MAAS_PASSWORD);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                MAAS_LOGIN_URL, HttpMethod.POST, request, String.class);

        log.info("MAAS 로그인 응답 상태: {}", response.getStatusCode());

        // 쿠키에서 csrftoken과 sessionid 추출
        HttpHeaders responseHeaders = response.getHeaders();
        Pattern csrfPattern = Pattern.compile("csrftoken=([^;]+)");
        Pattern sessionPattern = Pattern.compile("sessionid=([^;]+)");

        for (String cookie : responseHeaders.get("Set-Cookie")) {
            Matcher csrfMatcher = csrfPattern.matcher(cookie);
            if (csrfMatcher.find()) {
                this.csrfToken = csrfMatcher.group(1);
                log.info("CSRF 토큰 추출: {}", csrfToken);
            }
            
            Matcher sessionMatcher = sessionPattern.matcher(cookie);
            if (sessionMatcher.find()) {
                this.sessionId = sessionMatcher.group(1);
                log.info("세션 ID 추출: {}", sessionId);
            }
        }

        if (csrfToken == null || sessionId == null) {
            throw new RuntimeException("로그인 후 CSRF 토큰 또는 세션 ID를 찾을 수 없습니다.");
        }
    }

    /**
     * MAAS WebSocket 서버에 연결
     */
    public void connectToMaas(Consumer<String> messageCallback) {
        this.messageCallback = messageCallback;
        
        try {
            // 로그인하여 인증 정보 획득
            if (csrfToken == null || sessionId == null) {
                performLogin();
            }

            // WebSocket URL 구성
            String wsUrl = String.format("%s?csrftoken=%s", MAAS_WS_URL, csrfToken);
            
            log.info("MAAS WebSocket 연결 시도: {}", wsUrl);

            // WebSocket 리스너 생성
            WebSocketListener listener = new MaasWebSocketListener();

            // 연결 요청 생성 및 쿠키 헤더 추가
            org.eclipse.jetty.websocket.client.ClientUpgradeRequest upgradeRequest = 
                    new org.eclipse.jetty.websocket.client.ClientUpgradeRequest();
            
            // Cookie 헤더에 sessionid와 csrftoken 추가
            String cookieHeader = String.format("sessionid=%s; csrftoken=%s", sessionId, csrfToken);
            upgradeRequest.setHeader("Cookie", cookieHeader);
            upgradeRequest.setHeader("Origin", "http://" + MAAS_HOST);
            
            log.info("WebSocket 핸드셰이크 헤더 - Cookie: {}", cookieHeader);

            // WebSocket 연결
            maasSession = webSocketClient.connect(listener, new URI(wsUrl), upgradeRequest).get();
            connected = true;
            shouldReconnect = true; // 연결 성공 시 재연결 플래그 활성화
            lastActivityTime = System.currentTimeMillis();
            
            // Heartbeat 스케줄러 시작
            startHeartbeat();
            
            // 연결 후 초기 메시지 전송 (socket_gorila 참조)
            // 실시간 업데이트를 받기 위한 필수 메시지들 전송
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1초 대기
                    sendInitialMessages();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            log.info("MAAS WebSocket 연결 성공!");

        } catch (Exception e) {
            log.error("MAAS WebSocket 연결 실패", e);
            connected = false;
        }
    }

    /**
     * MAAS 서버로 메시지 전송
     */
    public void sendToMaas(String message) {
        if (maasSession != null && maasSession.isOpen()) {
            try {
                maasSession.getRemote().sendString(message);
                log.debug("MAAS로 메시지 전송: {}", message);
            } catch (Exception e) {
                log.error("MAAS로 메시지 전송 실패", e);
            }
        } else {
            log.warn("MAAS WebSocket이 연결되지 않았습니다.");
        }
    }

    public boolean isConnected() {
        return connected && maasSession != null && maasSession.isOpen();
    }

    /**
     * 초기 메시지 전송 (socket_gorila 참조)
     * 실시간 업데이트를 받기 위한 필수 메시지들 전송
     */
    private void sendInitialMessages() {
        if (!isConnected()) {
            log.warn("연결되지 않은 상태에서는 초기 메시지를 전송할 수 없습니다.");
            return;
        }
        
        try {
            long requestId = System.currentTimeMillis();
            
            // socket_gorila에서 사용하는 메시지들 전송
            // 1. user.auth_user
            String authMessage = String.format(
                "{\"method\":\"user.auth_user\",\"type\":0,\"request_id\":%d}",
                requestId++
            );
            sendToMaas(authMessage);
            log.debug("초기 메시지 전송 - user.auth_user");
            
            // 2. notification.list
            String notificationMessage = String.format(
                "{\"method\":\"notification.list\",\"type\":0,\"request_id\":%d}",
                requestId++
            );
            sendToMaas(notificationMessage);
            log.debug("초기 메시지 전송 - notification.list");
            
            // 3. resourcepool.list
            String poolMessage = String.format(
                "{\"method\":\"resourcepool.list\",\"type\":0,\"request_id\":%d}",
                requestId++
            );
            sendToMaas(poolMessage);
            log.debug("초기 메시지 전송 - resourcepool.list");
            
            // 4. machine.list - 이것이 중요! 실시간 업데이트를 받기 위한 구독
            String machineListMessage = String.format(
                "{\"method\":\"machine.list\",\"type\":0,\"request_id\":%d,\"params\":{\"filter\":{},\"group_collapsed\":[],\"group_key\":\"status\",\"page_number\":1,\"page_size\":50,\"sort_direction\":\"ascending\",\"sort_key\":\"hostname\"}}",
                requestId++
            );
            sendToMaas(machineListMessage);
            log.info("✅ 머신 목록 구독 메시지 전송 - 실시간 업데이트 활성화");
            
        } catch (Exception e) {
            log.error("초기 메시지 전송 실패", e);
        }
    }

    /**
     * Heartbeat 스케줄러 시작
     */
    private void startHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
        }
        
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                // 연결이 실제로 끊어진 경우에만 idle timeout 체크
                if (!isConnected()) {
                    log.debug("연결이 끊어진 상태 - heartbeat 전송 중단");
                    return;
                }
                
                long currentTime = System.currentTimeMillis();
                long idleTime = currentTime - lastActivityTime;
                
                // Idle timeout 체크 (연결이 실제로 열려있을 때만)
                if (idleTime > MAX_IDLE_TIME && isConnected()) {
                    log.warn("연결이 {}ms 동안 비활성 상태입니다. 재연결을 시도합니다.", idleTime);
                    shouldReconnect = true;
                    reconnectToMaas();
                    return;
                }
                
                // Heartbeat 메시지 전송 (재연결이 필요한 경우에만)
                if (isConnected() && shouldReconnect) {
                    String heartbeatMessage = "{\"method\":\"ping\",\"type\":0,\"request_id\":" + System.currentTimeMillis() + "}";
                    sendToMaas(heartbeatMessage);
                    log.debug("Heartbeat 전송: {}", heartbeatMessage);
                } else if (!shouldReconnect) {
                    log.debug("Heartbeat 중단됨 (재연결 불필요)");
                }
                
            } catch (Exception e) {
                log.error("Heartbeat 전송 중 오류 발생", e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        
        log.info("Heartbeat 스케줄러 시작 - 간격: {}ms, 최대 idle 시간: {}ms", HEARTBEAT_INTERVAL, MAX_IDLE_TIME);
    }

    /**
     * MAAS 서버 재연결
     */
    private void reconnectToMaas() {
        if (!shouldReconnect) {
            log.info("재연결 불필요 - 재연결하지 않음");
            return;
        }
        
        try {
            log.info("MAAS WebSocket 재연결 시도...");
            connected = false;
            
            if (maasSession != null && maasSession.isOpen()) {
                maasSession.close();
            }
            
            // 잠시 대기 후 재연결
            Thread.sleep(2000);
            
            // 재연결 전에 플래그 다시 확인
            if (shouldReconnect) {
                connectToMaas(messageCallback);
            }
            
        } catch (Exception e) {
            log.error("MAAS 재연결 실패", e);
        }
    }

    /**
     * MAAS WebSocket 리스너
     */
    private class MaasWebSocketListener implements WebSocketListener {

        @Override
        public void onWebSocketConnect(Session session) {
            log.info("MAAS WebSocket 연결됨: {}", session);
            connected = true;
        }

        @Override
        public void onWebSocketText(String message) {
            log.info("MAAS로부터 메시지 수신: {}", message);
            
            // 활동 시간 업데이트
            lastActivityTime = System.currentTimeMillis();
            
            // JSON 파싱하여 메시지 타입 확인
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(message);
                
                // pong 메시지는 heartbeat 응답이므로 프론트엔드로 전달하지 않음
                if (jsonNode.has("method")) {
                    String method = jsonNode.get("method").asText();
                    if ("pong".equals(method)) {
                        log.debug("💓 Pong 메시지 수신 (heartbeat 응답) - 프론트엔드로 전달하지 않음");
                        return; // pong은 프론트엔드로 전달하지 않음
                    }
                }
                
                if (jsonNode.has("name")) {
                    String name = jsonNode.get("name").asText();
                    String action = jsonNode.has("action") ? jsonNode.get("action").asText() : "N/A";
                    int type = jsonNode.has("type") ? jsonNode.get("type").asInt() : -1;
                    
                    log.info("📋 메시지 분석 - name: {}, action: {}, type: {}", name, action, type);
                    
                    if ("machine".equals(name)) {
                        log.info("✅ 머신 이벤트 수신! action: {}, full message: {}", action, message);
                        if (jsonNode.has("data")) {
                            com.fasterxml.jackson.databind.JsonNode data = jsonNode.get("data");
                            if (data.has("system_id")) {
                                log.info("✅ 머신 system_id: {}, status: {}", 
                                    data.get("system_id").asText(),
                                    data.has("status") ? data.get("status").asText() : "N/A");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("메시지 파싱 실패 (비JSON 메시지일 수 있음): {}", e.getMessage());
            }
            
            // pong이 아닌 메시지만 프론트엔드로 전달
            if (messageCallback != null) {
                messageCallback.accept(message);
            }
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            log.debug("MAAS로부터 바이너리 메시지 수신 (길이: {})", len);
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            log.warn("MAAS WebSocket 연결 종료 - 코드: {}, 이유: {}", statusCode, reason);
            connected = false;
            
            // Heartbeat 스케줄러 중지
            if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
                heartbeatScheduler.shutdown();
            }
            
            // 특정 에러 코드는 재연결하지 않음
            // 1000 = 정상 종료 (클로즈 프레임)
            // 1002 = Protocol Error (Invalid method formatting 등) - 재연결해도 같은 문제 반복
            // 1003 = Unsupported Data (데이터 타입 오류)
            // 1007 = Invalid frame payload data (프레임 데이터 오류)
            if (statusCode == 1000 || statusCode == 1002 || statusCode == 1003 || statusCode == 1007) {
                log.warn("에러 코드 {}: 재연결하지 않음 (재연결해도 같은 문제가 반복될 수 있음)", statusCode);
                shouldReconnect = false;
                return;
            }
            
            // 그 외의 비정상 종료인 경우에만 재연결 시도
            if (shouldReconnect) {
                log.info("비정상 종료 감지 - 재연결 시도 예정 (5초 후)");
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        // 재연결 전에 연결 상태 다시 확인
                        if (!isConnected() && shouldReconnect) {
                            log.info("MAAS WebSocket 재연결 시도...");
                            connectToMaas(messageCallback);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                log.info("재연결 불필요 - 재연결하지 않음");
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            log.error("MAAS WebSocket 오류", cause);
            connected = false;
        }
    }
}