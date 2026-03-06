package com.smartmes.backend.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartMesOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Smart MES API - Đồ án tốt nghiệp")
                        .description("Tài liệu API cho hệ thống Quản lý Thực thi Sản xuất thông minh (Smart MES). Tích hợp Real-time & Offline-first.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Nguyễn Chương Hào")
                                .email("hao.nguyen@example.com"))
                        .license(new License().name("Đại học Công nghiệp Hà Nội").url("https://haui.edu.vn")));
    }
}