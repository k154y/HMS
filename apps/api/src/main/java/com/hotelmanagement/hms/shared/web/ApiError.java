package com.hotelmanagement.hms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message,
                       String path, String requestId, List<FieldError> fieldErrors) {
    public static final String AUTHENTICATION_MESSAGE = "Your session has expired or is no longer valid. Please sign in again.";
    public static final String AUTHORIZATION_MESSAGE = "You don't have permission to perform this action.";
    public static final String NOT_FOUND_MESSAGE = "The requested record could not be found or is no longer available.";
    public record FieldError(String field, String message) {}
    public static ApiError of(HttpServletRequest request, int status, String code, String message,
                              List<FieldError> fields) {
        return new ApiError(Instant.now(), status, code, message, request.getRequestURI(),
                RequestCorrelationFilter.reference(request), fields);
    }
}
