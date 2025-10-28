package com.maas.ui.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class MaasAuthService {
    
    private static final String OAUTH_VERSION = "1.0";
    private static final String OAUTH_SIGNATURE_METHOD = "PLAINTEXT";
    
    /**
     * MAAS API OAuth 1.0 인증 헤더를 생성합니다.
     * 
     * @param apiKey MAAS API 키 (consumer_key:token:token_secret 형식)
     * @return OAuth 인증 헤더 문자열
     */
    public String generateAuthHeader(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        String[] keyParts = apiKey.split(":");
        if (keyParts.length != 3) {
            throw new IllegalArgumentException("API key must be in format: consumer_key:token:token_secret");
        }
        
        String consumerKey = keyParts[0];
        String token = keyParts[1];
        String tokenSecret = keyParts[2];
        
        // UUID v4 생성 (nonce)
        String nonce = UUID.randomUUID().toString();
        
        // 현재 시간 (timestamp)
        long timestamp = System.currentTimeMillis() / 1000;
        
        // 서명 생성 (PLAINTEXT 방식)
        String signature = "&" + tokenSecret;
        
        // OAuth 헤더 생성
        return String.format(
            "OAuth oauth_version=\"%s\", oauth_signature_method=\"%s\", oauth_consumer_key=\"%s\", oauth_token=\"%s\", oauth_signature=\"%s\", oauth_nonce=\"%s\", oauth_timestamp=\"%d\"",
            OAUTH_VERSION,
            OAUTH_SIGNATURE_METHOD,
            consumerKey,
            token,
            signature,
            nonce,
            timestamp
        );
    }
    
    /**
     * MAAS API 키의 유효성을 검증합니다.
     * 
     * @param apiKey 검증할 API 키
     * @return 유효한 형식이면 true, 그렇지 않으면 false
     */
    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        
        String[] keyParts = apiKey.split(":");
        return keyParts.length == 3 && 
               !keyParts[0].isEmpty() && 
               !keyParts[1].isEmpty() && 
               !keyParts[2].isEmpty();
    }
}
