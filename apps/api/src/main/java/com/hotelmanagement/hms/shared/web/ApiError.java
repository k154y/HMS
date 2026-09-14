package com.hotelmanagement.hms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message,
                       String path, String requestId, List<FieldError> fieldErrors) {
    public record FieldError(String field, String message) {}
    public static ApiError of(HttpServletRequest request, int status, String code, String message,
                              List<FieldError> fields) {
        return new ApiError(Instant.now(), status, code, message, request.getRequestURI(),
                (String) request.getAttribute(RequestCorrelationFilter.ATTRIBUTE), fields);
    }
}
