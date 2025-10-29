package com.maas.ui.handler;

import com.maas.ui.service.MaasWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ClientWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private MaasWebSocketService maasWebSocketService;
    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("프론트엔드 클라이언트 연결됨: {}", session.getId());
        clientSessions.put(session.getId(), session);
        
        // MAAS 서버에 연결 (아직 연결되지 않은 경우)
        if (!maasWebSocketService.isConnected()) {
            maasWebSocketService.connectToMaas(this::broadcastToClients);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("프론트엔드로부터 메시지 수신: {}", payload);
        
        // Heartbeat 메시지 처리
        if (payload.contains("\"method\":\"ping\"")) {
            log.debug("Heartbeat 메시지 수신 - 응답 전송");
            // Heartbeat 응답 전송
            String pongMessage = payload.replace("\"method\":\"ping\"", "\"method\":\"pong\"");
            session.sendMessage(new TextMessage(pongMessage));
            return;
        }
        
        // 일반 메시지는 MAAS 서버로 전달
        maasWebSocketService.sendToMaas(payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("프론트엔드 클라이언트 연결 종료: {}, 상태: {}", session.getId(), status);
        clientSessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류: {}", session.getId(), exception);
        clientSessions.remove(session.getId());
    }

    /**
     * MAAS 서버로부터 받은 메시지를 모든 연결된 프론트엔드 클라이언트에게 브로드캐스트
     */
    public void broadcastToClients(String message) {
        log.info("브로드캐스트 - 클라이언트 수: {}, 메시지: {}", clientSessions.size(), message);
        
        for (WebSocketSession session : clientSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("클라이언트에게 메시지 전송 실패: {}", session.getId(), e);
                }
            }
        }
    }
}
