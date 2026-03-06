package com.smartmes.backend.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt bảo vệ CSRF (bắt buộc tắt thì Swagger/Postman mới gửi lệnh POST được)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Cấu hình phân quyền đường dẫn
            .authorizeHttpRequests(auth -> auth
                // 1. Mở khóa hoàn toàn cho các đường dẫn của Swagger UI
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                
                // 2. TẠM THỜI mở khóa cho tất cả các API của chúng ta để dễ test.
                // (Sau này làm đến Phân hệ Đăng nhập, chúng ta sẽ xóa dòng này đi để khóa lại)
                .requestMatchers("/api/v1/**").permitAll()
                
                // Các đường dẫn khác thì bắt buộc đăng nhập
                .anyRequest().authenticated()
            );

        return http.build();
    }
}