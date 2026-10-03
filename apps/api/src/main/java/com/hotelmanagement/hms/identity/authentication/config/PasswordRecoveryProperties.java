package com.hotelmanagement.hms.identity.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@ConfigurationProperties("hms.security.recovery")
public record PasswordRecoveryProperties(boolean enabled, String frontendUrl, String from) {
    @Configuration
    @EnableAsync
    @EnableConfigurationProperties(PasswordRecoveryProperties.class)
    public static class Config {}
}
