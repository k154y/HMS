package com.hotelmanagement.hms.identity.authentication.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Validated
@ConfigurationProperties("hms.security.api")
public record ApiSecurityProperties(@NotEmpty List<@NotBlank String> allowedOrigins,
                                    @Min(1) int authRequestsPerMinute) {
    public ApiSecurityProperties {
        if (allowedOrigins != null && allowedOrigins.stream().anyMatch(s -> s.contains("*")))
            throw new IllegalArgumentException("CORS origins must be explicit.");
    }
}
