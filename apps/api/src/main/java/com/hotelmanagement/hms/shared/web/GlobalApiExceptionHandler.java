package com.hotelmanagement.hms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    protected ResponseEntity<ApiError> domain(ApiException exception, HttpServletRequest request) {
        if (exception.status() >= 500) return unexpected(exception, request);
        return response(request, exception.status(), exception.code(), exception.getMessage());
    }
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
        return response(request, 401, "AUTHENTICATION_FAILED", ApiError.AUTHENTICATION_MESSAGE);
    }
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return response(request, 403, "ACCESS_DENIED", ApiError.AUTHORIZATION_MESSAGE);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiError> missing(NoResourceFoundException exception, HttpServletRequest request) {
        return response(request, 404, "RESOURCE_NOT_FOUND", ApiError.NOT_FOUND_MESSAGE);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiError.of(request, 400, "VALIDATION_FAILED",
                "Please correct the highlighted fields.", fields));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiError> constraints(ConstraintViolationException exception, HttpServletRequest request) {
        var fields = exception.getConstraintViolations().stream()
                .map(error -> new ApiError.FieldError(error.getPropertyPath().toString(), error.getMessage())).toList();
        return ResponseEntity.badRequest().body(ApiError.of(request, 400, "VALIDATION_FAILED",
                "Please correct the highlighted fields.", fields));
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    protected ResponseEntity<ApiError> invalid(Exception exception, HttpServletRequest request) {
        return response(request, 400, "VALIDATION_FAILED", "Please check the information you entered and try again.");
    }
    @ExceptionHandler(BusinessRuleException.class)
    protected ResponseEntity<ApiError> business(BusinessRuleException exception, HttpServletRequest request) {
        return response(request, 409, exception.code(), exception.getMessage());
    }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    protected ResponseEntity<ApiError> conflict(Exception exception, HttpServletRequest request) {
        if (exception instanceof DataIntegrityViolationException integrity
                && integrity.getMostSpecificCause() instanceof java.sql.SQLException sql
                && "23P01".equals(sql.getSQLState()))
            return response(request,409,"ROOM_UNAVAILABLE","This room is no longer available for the selected dates.");
        return response(request, 409, "STATE_CONFLICT",
                "This action conflicts with the current record. Refresh the page and review your changes before trying again.");
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    protected ResponseEntity<ApiError> responseStatus(
            org.springframework.web.server.ResponseStatusException exception,
            HttpServletRequest request) {

        int status = exception.getStatusCode().value();

        return response(
                request,
                status,
                status == 403 ? "ACCESS_DENIED" : "REQUEST_REJECTED",
                status == 403
                        ? ApiError.AUTHORIZATION_MESSAGE
                        : "The request could not be completed."
        );
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        String id = RequestCorrelationFilter.reference(request);
        // Messages, causes, request bodies and SQL can contain credentials or account identifiers.
        // Keep diagnostic type and stack locations, but never log their arbitrary text.
        var frames = java.util.Arrays.stream(exception.getStackTrace()).limit(30)
                .map(frame -> frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber()).toList();
        LoggerFactory.getLogger(getClass()).error("event=unexpected_api_failure requestId={} exceptionType={} frames={}",
                id, exception.getClass().getName(), frames);
        int status = exception instanceof ApiException domain ? domain.status() : 500;
        String code = exception instanceof ApiException domain && status != 500 ? domain.code() : "INTERNAL_ERROR";
        return response(request, status, code,
                "We couldn't complete this action because of a system problem. Please contact the system administrator and provide reference ID: " + id + ".");
    }
    private ResponseEntity<ApiError> response(HttpServletRequest request, int status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(request, status, code, message, List.of()));
    }
}
