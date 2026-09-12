package com.hotelmanagement.hms.identity.authentication.config;

import com.hotelmanagement.hms.identity.authentication.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthenticationProperties.class
})
public class JwtConfiguration {

    private static final int MINIMUM_HS256_SECRET_BYTES = 32;

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

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer())
        );

        return decoder;
    }
}