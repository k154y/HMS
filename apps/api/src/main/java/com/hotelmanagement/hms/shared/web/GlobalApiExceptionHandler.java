package com.hotelmanagement.hms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> domain(ApiException ex, HttpServletRequest request) {
        return response(request, ex.status(), ex.code(), ex.getMessage());
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest request) {
        return response(request, 401, "AUTHENTICATION_FAILED", "Authentication failed.");
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException ex, HttpServletRequest request) {
        return response(request, 403, "ACCESS_DENIED", "Access denied.");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiError.of(request, 400, "VALIDATION_FAILED",
                "One or more fields are invalid.", fields));
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid(Exception ex, HttpServletRequest request) {
        return response(request, 400, "INVALID_REQUEST", "The request is invalid.");
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> state(IllegalStateException ex,HttpServletRequest request){
        boolean domain=ex.getStackTrace().length>0&&ex.getStackTrace()[0].getClassName().startsWith("com.hotelmanagement.hms.");
        return response(request,409,"STATE_CONFLICT",domain&&ex.getMessage()!=null?ex.getMessage():"The operation conflicts with current state.");
    }
    @ExceptionHandler({DataIntegrityViolationException.class,
            ConcurrencyFailureException.class})
    ResponseEntity<ApiError> conflict(Exception ex, HttpServletRequest request) {
        return response(request, 409, "STATE_CONFLICT", "The operation conflicts with current state.");
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        // Never include request bodies or exception messages that could contain credentials.
        log.error("event=unexpected_api_failure exceptionType={}", ex.getClass().getName());
        return response(request, 500, "INTERNAL_ERROR", "An unexpected error occurred.");
    }
    private ResponseEntity<ApiError> response(HttpServletRequest request, int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(request, status, code, message, List.of()));
    }
}
