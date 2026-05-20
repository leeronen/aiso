package com.aios.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aios.jwt")
public class JwtProperties {

    private String secret = "change-me";
    private long accessTokenMinutes = 120;
    private long refreshTokenDays = 14;
}
