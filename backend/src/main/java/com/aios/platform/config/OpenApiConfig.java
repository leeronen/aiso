package com.aios.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_BEARER = "bearer-jwt";

    @Bean
    public OpenAPI aiosOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("AIOS Platform API")
                                .description(
                                        "企业级 AI Agent 管理平台 REST API。"
                                                + " 除 `/api/auth/**` 外，请在 Swagger UI 右上角 Authorize 填入：`Bearer <accessToken>`。")
                                .version("0.1.0")
                                .contact(new Contact().name("AIOS").email("admin@aios.local"))
                                .license(new License().name("Apache 2.0")))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_BEARER,
                                        new SecurityScheme()
                                                .name(SECURITY_SCHEME_BEARER)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("登录接口返回的 accessToken")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER));
    }
}
