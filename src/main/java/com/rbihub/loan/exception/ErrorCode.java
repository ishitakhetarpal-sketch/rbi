package com.rbihub.loan.exception;

/**
 * Stable, machine-readable application error codes returned in the
 * {@code errorCode} field. Clients should branch on these, not on the human
 * {@code message}.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    CONSTRAINT_VIOLATION,
    MALFORMED_REQUEST,
    INVALID_ENUM_VALUE,
    MISSING_PARAMETER,
    TYPE_MISMATCH,
    RESOURCE_NOT_FOUND,
    ILLEGAL_ARGUMENT,
    INTERNAL_ERROR
}
