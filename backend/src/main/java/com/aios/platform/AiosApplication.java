package com.aios.platform;

import com.aios.platform.config.JvmNetworkBootstrap;
import com.aios.platform.security.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.aios.platform")
@EnableConfigurationProperties(JwtProperties.class)
public class AiosApplication {

    public static void main(String[] args) {
        JvmNetworkBootstrap.bypassProxyForLocalServices();
        SpringApplication.run(AiosApplication.class, args);
    }
}
