package com.rbihub.loan.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Canonical error response. Fields are nullable so handlers can omit anything
 * irrelevant (e.g. {@code fieldErrors} only fires for validation failures).
 *
 * @param timestamp   when the error was produced
 * @param status      HTTP status code
 * @param error       HTTP reason phrase (e.g. {@code "Bad Request"})
 * @param errorCode   machine-readable application code (e.g. {@code VALIDATION_FAILED})
 * @param message     human-readable description
 * @param path        request path that produced the error
 * @param method      HTTP method
 * @param traceId     correlation id; present on every response, log it for support
 * @param fieldErrors per-field detail for validation failures
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        String method,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, Object rejectedValue, String message) {
    }
}
