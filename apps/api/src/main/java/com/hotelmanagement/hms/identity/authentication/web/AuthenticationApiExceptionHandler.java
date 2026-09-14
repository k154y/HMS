package com.hotelmanagement.hms.identity.authentication.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(
        assignableTypes =
                AuthenticationController.class
)
public class AuthenticationApiExceptionHandler {

    /**
     * Covers invalid credentials, locked accounts,
     * disabled accounts, invalid refresh credentials,
     * expired refresh sessions, and similar authentication
     * failures.
     *
     * The response is deliberately generic.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>>
    handleAuthenticationFailure(
            AuthenticationException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed.",
                request,
                List.of()
        );
    }

    /**
     * Handles Jakarta Bean Validation failures such as:
     *
     * - missing email;
     * - malformed email;
     * - missing password;
     * - missing refresh token.
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<Map<String, String>> fieldErrors =
                new ArrayList<>();

        for (FieldError fieldError
                : exception
                        .getBindingResult()
                        .getFieldErrors()) {

            Map<String, String> error =
                    new LinkedHashMap<>();

            error.put(
                    "field",
                    fieldError.getField()
            );

            error.put(
                    "message",
                    fieldError.getDefaultMessage()
            );

            fieldErrors.add(error);
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "One or more request fields are invalid.",
                request,
                fieldErrors
        );
    }

    /**
     * Handles malformed or unreadable JSON request bodies.
     */
    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "The request body is invalid.",
                request,
                List.of()
        );
    }

    private ResponseEntity<Map<String, Object>>
    buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<?> fieldErrors) {

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
                status.value()
        );

        body.put(
                "code",
                code
        );

        body.put(
                "message",
                message
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
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}