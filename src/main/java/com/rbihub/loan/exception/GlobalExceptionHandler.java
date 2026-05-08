package com.rbihub.loan.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rbihub.loan.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toApiFieldError)
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Validation failed for one or more fields", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
                .map(this::toApiFieldError)
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.CONSTRAINT_VIOLATION,
                "Constraint violation on request parameters", req, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest req) {
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType() != null) {
            String field = pathOf(ife);
            String message = "Invalid value for field '" + field + "': expected " + ife.getTargetType().getSimpleName();
            ErrorResponse.FieldError fe = new ErrorResponse.FieldError(field, ife.getValue(), message);
            return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ENUM_VALUE, message, req, List.of(fe));
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "Malformed JSON request", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        String message = "Missing required request parameter '" + ex.getParameterName() + "'";
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_PARAMETER, message, req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type";
        String message = "Parameter '" + ex.getName() + "' could not be converted to " + expected;
        ErrorResponse.FieldError fe = new ErrorResponse.FieldError(ex.getName(), ex.getValue(), message);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.TYPE_MISMATCH, message, req, List.of(fe));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest req) {
        String message = "No endpoint mapped for " + req.getMethod() + " " + req.getRequestURI();
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, message, req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.ILLEGAL_ARGUMENT,
                ex.getMessage() == null ? "Illegal argument" : ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.error("[traceId={}] Unhandled exception while processing {} {}",
                traceId, req.getMethod(), req.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please contact support with the traceId.",
                req.getRequestURI(),
                req.getMethod(),
                traceId,
                null
        );
        return ResponseEntity.internalServerError().body(body);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                ErrorCode code,
                                                String message,
                                                HttpServletRequest req,
                                                List<ErrorResponse.FieldError> fields) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                req.getRequestURI(),
                req.getMethod(),
                traceId(),
                fields == null || fields.isEmpty() ? null : fields
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }

    private ErrorResponse.FieldError toApiFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage());
    }

    private ErrorResponse.FieldError toApiFieldError(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        return new ErrorResponse.FieldError(path, v.getInvalidValue(), v.getMessage());
    }

    private static String pathOf(InvalidFormatException ife) {
        return ife.getPath().stream()
                .map(ref -> ref.getFieldName() == null ? "[" + ref.getIndex() + "]" : ref.getFieldName())
                .collect(Collectors.joining("."));
    }
}
