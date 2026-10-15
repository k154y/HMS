package com.hotelmanagement.hms.identity.authentication.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

/** Blocks operational APIs while a KBP-provisioned owner's temporary password is active. */
public final class TemporaryPasswordGate extends OncePerRequestFilter {
    private final JdbcTemplate db;
    public TemporaryPasswordGate(JdbcTemplate db){this.db=db;}
    @Override protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        String path=request.getRequestURI();
        if(auth instanceof JwtAuthenticationToken jwt &&
                !path.equals("/api/v1/auth/me") &&
                !path.equals("/api/v1/auth/password") &&
                !path.equals("/api/v1/auth/logout") &&
                !path.equals("/actuator/health")) {
            UUID user=UUID.fromString(jwt.getToken().getSubject());
            if(Boolean.TRUE.equals(db.queryForObject(
                    "SELECT must_change_password FROM users WHERE id=?",Boolean.class,user))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setHeader("Cache-Control","no-store");
                response.getWriter().write(
                    "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Change your temporary password before using the platform.\"}");
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
