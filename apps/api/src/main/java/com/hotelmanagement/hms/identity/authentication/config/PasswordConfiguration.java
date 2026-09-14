package com.hotelmanagement.hms.identity.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PasswordConfiguration {

    private static final String CURRENT_ENCODER_ID =
            "pbkdf2-hmac-sha256-600k";

    private static final int PBKDF2_SALT_LENGTH_BYTES =
            16;

    private static final int PBKDF2_ITERATIONS =
            600_000;

    @Bean
    public PasswordEncoder passwordEncoder() {

        Pbkdf2PasswordEncoder currentEncoder =
                new Pbkdf2PasswordEncoder(
                        "",
                        PBKDF2_SALT_LENGTH_BYTES,
                        PBKDF2_ITERATIONS,
                        Pbkdf2PasswordEncoder
                                .SecretKeyFactoryAlgorithm
                                .PBKDF2WithHmacSHA256
                );

        Map<String, PasswordEncoder> encoders =
                new HashMap<>();

        /*
         * Encoder used for every NEW password.
         */
        encoders.put(
                CURRENT_ENCODER_ID,
                currentEncoder
        );

        /*
         * Keep bcrypt verification support because previous
         * development versions used Spring's default delegating
         * password encoder, which may have produced {bcrypt}
         * hashes.
         *
         * New HMS passwords are NOT encoded with bcrypt.
         */
        encoders.put(
                "bcrypt",
                new BCryptPasswordEncoder()
        );

        /*
         * Also retain Spring's PBKDF2 v5.8 identifier so an
         * existing Spring-generated PBKDF2 hash can still be
         * verified if one appears during development/migration.
         */
        encoders.put(
                "pbkdf2@SpringSecurity_v5_8",
                Pbkdf2PasswordEncoder
                        .defaultsForSpringSecurity_v5_8()
        );

        return new DelegatingPasswordEncoder(
                CURRENT_ENCODER_ID,
                encoders
        );
    }
}