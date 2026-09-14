package com.hotelmanagement.hms.identity.authentication.config;

import com.hotelmanagement.hms.identity.authentication.web.RestAccessDeniedHandler;
import com.hotelmanagement.hms.identity.authentication.web.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(ApiSecurityProperties.class)
public class SecurityConfiguration {

    private final RestAuthenticationEntryPoint
            authenticationEntryPoint;

    private final RestAccessDeniedHandler
            accessDeniedHandler;

    public SecurityConfiguration(
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {

        this.authenticationEntryPoint =
                authenticationEntryPoint;

        this.accessDeniedHandler =
                accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiSecurityProperties apiProperties,
            org.springframework.data.redis.core.StringRedisTemplate redis,
            tools.jackson.databind.json.JsonMapper mapper,
            com.hotelmanagement.hms.identity.repository.UserRepository users)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(apiProperties.allowedOrigins());
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Request-ID", "Idempotency-Key"));
                    config.setExposedHeaders(java.util.List.of("X-Request-ID", "Retry-After"));
                    config.setAllowCredentials(false);
                    return config;
                }))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new com.hotelmanagement.hms.identity.authentication.web.AuthenticationRateLimitFilter(
                        redis, apiProperties, mapper), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                /*
                 * The HMS API authenticates with bearer tokens rather
                 * than browser cookie sessions.
                 *
                 * If we later move authentication credentials into
                 * cookies, the CSRF strategy must be reviewed.
                 */
                .csrf(
                        AbstractHttpConfigurer::disable
                )

                /*
                 * Do not store authentication in an HTTP session.
                 * Every protected request must carry its bearer token.
                 */
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS)
                )

                /*
                 * We do not need Spring Security to remember and
                 * redirect to previously requested pages because
                 * this is a JSON API, not form-based authentication.
                 */
                .requestCache(
                        AbstractHttpConfigurer::disable
                )

                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )

                .authorizeHttpRequests(
                        authorization ->
                                authorization

                                        /*
                                         * Health monitoring must work
                                         * without an HMS user account.
                                         */
                                        .requestMatchers(
                                                "/actuator/health"
                                        )
                                        .permitAll()

                                        /* Public credential exchange and documented self-service signup. */
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/refresh",
                                                "/api/v1/onboarding/signup"
                                        )
                                        .permitAll()

                                        // Operational metrics require a separate deployment monitoring boundary.
                                        .requestMatchers("/actuator/**").denyAll()

                                        /*
                                         * Every other API/resource
                                         * requires a validated bearer
                                         * access token.
                                         */
                                        .anyRequest()
                                        .authenticated()
                )

                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2
                                        .jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(token -> {
                                                    var id = java.util.UUID.fromString(token.getSubject());
                                                    if (users.findByIdAndStatus(id,
                                                            com.hotelmanagement.hms.identity.model.UserStatus.ACTIVE).isEmpty())
                                                        throw new org.springframework.security.authentication.BadCredentialsException("Account is not active.");
                                                    return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                                                            token, java.util.List.of());
                                                })
                                        )
                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )
                                        .accessDeniedHandler(
                                                accessDeniedHandler
                                        )
                );

        return http.build();
    }
}
