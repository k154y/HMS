package com.hotelmanagement.hms.identity.authentication.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(
        prefix = "hms.security.authentication"
)
public record AuthenticationProperties(

        @Min(1)
        int maxFailedAttempts,

        @Min(1)
        long lockMinutes
) {
}