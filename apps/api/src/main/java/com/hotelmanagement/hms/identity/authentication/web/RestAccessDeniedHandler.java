package com.hotelmanagement.hms.identity.authentication.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    private final BearerTokenAccessDeniedHandler
            bearerAccessDeniedHandler =
            new BearerTokenAccessDeniedHandler();

    public RestAccessDeniedHandler(
            JsonMapper jsonMapper) {

        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        bearerAccessDeniedHandler.handle(
                request,
                response,
                accessDeniedException
        );

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN);

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
                HttpServletResponse.SC_FORBIDDEN
        );

        body.put(
                "code",
                "ACCESS_DENIED"
        );

        body.put(
                "message",
                "You are not permitted to perform "
                        + "this operation."
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