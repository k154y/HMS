package com.hotelmanagement.hms.identity.authentication.config;

import com.hotelmanagement.hms.identity.authentication.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthenticationProperties.class
})
public class JwtConfiguration {

    private static final int MINIMUM_HS256_SECRET_BYTES = 32;

    private static final String TOKEN_TYPE_CLAIM =
            "token_type";

    private static final String ACCESS_TOKEN_TYPE =
            "access";

    private final JwtProperties properties;

    public JwtConfiguration(
            JwtProperties properties) {

        this.properties = properties;
    }

    @Bean
    public SecretKey jwtSecretKey() {

        byte[] secretBytes =
                properties.secret()
                        .getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length
                < MINIMUM_HS256_SECRET_BYTES) {

            throw new IllegalStateException(
                    "HMS_JWT_SECRET must contain at least "
                            + MINIMUM_HS256_SECRET_BYTES
                            + " bytes for HS256.");
        }

        return new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(
            SecretKey jwtSecretKey) {

        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(jwtSecretKey)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        OAuth2TokenValidator<Jwt> standardValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer());

        OAuth2TokenValidator<Jwt> accessTokenValidator =
                new JwtClaimValidator<String>(
                        TOKEN_TYPE_CLAIM,
                        ACCESS_TOKEN_TYPE::equals
                );

        OAuth2TokenValidator<Jwt> subjectValidator =
                new JwtClaimValidator<String>(
                        "sub",
                        JwtConfiguration::isValidUserId
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        standardValidator,
                        accessTokenValidator,
                        subjectValidator,
                        new JwtClaimValidator<Object>("exp", java.util.Objects::nonNull),
                        new JwtClaimValidator<Object>("iat", java.util.Objects::nonNull)
                )
        );

        return decoder;
    }

    private static boolean isValidUserId(
            String subject) {

        if (subject == null
                || subject.isBlank()) {

            return false;
        }

        try {
            return UUID.fromString(subject).toString().equalsIgnoreCase(subject);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
