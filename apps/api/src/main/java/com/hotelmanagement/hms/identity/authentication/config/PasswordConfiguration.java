package com.hotelmanagement.hms.identity.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfiguration {

    /**
     * Creates the application's password encoder.
     *
     * DelegatingPasswordEncoder stores an algorithm identifier
     * with each encoded password. This allows the application
     * to evolve its password hashing strategy later without
     * invalidating existing accounts.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }
}