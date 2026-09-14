package com.hotelmanagement.hms.identity.authentication.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    private final BearerTokenAuthenticationEntryPoint
            bearerEntryPoint =
            new BearerTokenAuthenticationEntryPoint();

    public RestAuthenticationEntryPoint(
            JsonMapper jsonMapper) {

        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {

        /*
         * Preserve Spring Security's Bearer authentication headers
         * and RFC-compliant HTTP status handling.
         */
        bearerEntryPoint.commence(
                request,
                response,
                authenticationException
        );

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED);

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                OffsetDateTime
                        .now(ZoneOffset.UTC)
                        .toString()
        );

        body.put(
                "status",
                HttpServletResponse.SC_UNAUTHORIZED
        );

        body.put(
                "code",
                "UNAUTHORIZED"
        );

        body.put(
                "message",
                "Authentication is required or "
                        + "the access token is invalid."
        );

        body.put(
                "path",
                request.getRequestURI()
        );

        body.put(
                "requestId",
                request.getAttribute(
                        "hms.requestId")
        );

        body.put(
                "fieldErrors",
                List.of()
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}