package com.hotelmanagement.hms.identity.authentication.web;

import com.hotelmanagement.hms.identity.authentication.config.ApiSecurityProperties;
import com.hotelmanagement.hms.shared.web.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh",
            "/api/v1/auth/password", "/api/v1/onboarding/signup", "/api/v1/onboarding/hotels");
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]); if n == 1 then redis.call('EXPIRE', KEYS[1], 60) end; return n", Long.class);
    private final StringRedisTemplate redis;
    private final ApiSecurityProperties properties;
    private final JsonMapper mapper;
    public AuthenticationRateLimitFilter(StringRedisTemplate redis, ApiSecurityProperties properties, JsonMapper mapper) {
        this.redis = redis; this.properties = properties; this.mapper = mapper;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATHS.contains(request.getServletPath());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws IOException, ServletException {
        Long count;
        try {
            // The socket peer is authoritative. Forwarded headers require trusted proxy configuration.
            count = redis.execute(SCRIPT, List.of("hms:auth-limit:" + request.getRemoteAddr()));
        } catch (org.springframework.dao.DataAccessException ex) {
            reject(request, response, 503, "AUTH_LIMITER_UNAVAILABLE");
            return;
        }
        if (count == null || count > properties.authRequestsPerMinute()) {
            reject(request, response, 429, "RATE_LIMITED");
            return;
        }
        chain.doFilter(request, response);
    }
    private void reject(HttpServletRequest request, HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ApiError.of(request, status, code,
                "Please retry later.", List.of()));
    }
}
