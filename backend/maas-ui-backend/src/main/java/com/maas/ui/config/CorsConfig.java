package com.maas.ui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 모든 origin 허용 (개발용 - 프로덕션에서는 특정 도메인만 허용)
        config.addAllowedOriginPattern("*");
        
        // 모든 헤더 허용
        config.addAllowedHeader("*");
        
        // 모든 HTTP 메서드 허용
        config.addAllowedMethod("*");
        
        // 인증 정보 허용
        config.setAllowCredentials(true);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

