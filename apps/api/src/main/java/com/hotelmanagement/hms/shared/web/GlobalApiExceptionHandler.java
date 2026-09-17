package com.hotelmanagement.hms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalApiExceptionHandler.class
            );

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> domain(
            ApiException exception,
            HttpServletRequest request) {

        return response(
                request,
                exception.status(),
                exception.code(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(
            AuthenticationException exception,
            HttpServletRequest request) {

        return response(
                request,
                401,
                "AUTHENTICATION_FAILED",
                "Authentication failed."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return response(
                request,
                403,
                "ACCESS_DENIED",
                "Access denied."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<ApiError.FieldError> fields =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                new ApiError.FieldError(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                        )
                        .toList();

        return ResponseEntity
                .badRequest()
                .body(
                        ApiError.of(
                                request,
                                400,
                                "VALIDATION_FAILED",
                                "Please correct the highlighted fields.",
                                fields
                        )
                );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiError> invalid(
            Exception exception,
            HttpServletRequest request) {

        return response(
                request,
                400,
                "INVALID_REQUEST",
                "The request could not be processed. Please check the information you entered."
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> state(
            IllegalStateException exception,
            HttpServletRequest request) {

        boolean domainException =
                exception.getStackTrace().length > 0
                        && exception
                        .getStackTrace()[0]
                        .getClassName()
                        .startsWith(
                                "com.hotelmanagement.hms."
                        );

        String message =
                domainException
                        && exception.getMessage() != null
                        && !exception.getMessage().isBlank()
                        ? exception.getMessage()
                        : "The operation conflicts with the current state.";

        return response(
                request,
                409,
                "STATE_CONFLICT",
                message
        );
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            ConcurrencyFailureException.class
    })
    ResponseEntity<ApiError> conflict(
            Exception exception,
            HttpServletRequest request) {

        return response(
                request,
                409,
                "STATE_CONFLICT",
                "The operation conflicts with the current state."
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(
            Exception exception,
            HttpServletRequest request) {

        /*
         * Never send raw exception messages to the client here.
         *
         * They may contain database details, infrastructure
         * information, credentials, or other internal data.
         */
        log.error(
                "event=unexpected_api_failure exceptionType={}",
                exception.getClass().getName()
        );

        return response(
                request,
                500,
                "INTERNAL_ERROR",
                "An unexpected error occurred."
        );
    }

    private ResponseEntity<ApiError> response(
            HttpServletRequest request,
            int status,
            String code,
            String message) {

        return ResponseEntity
                .status(status)
                .body(
                        ApiError.of(
                                request,
                                status,
                                code,
                                message,
                                List.of()
                        )
                );
    }
}