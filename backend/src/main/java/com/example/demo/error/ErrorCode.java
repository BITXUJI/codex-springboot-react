package com.example.demo.error;

/**
 * Standardized error codes for API responses.
 * 
 * <pre>
 * Usage:
 * 1) Map exception scenarios to a stable symbolic value.
 * 2) Serialize the enum name in error response payloads.
 * 3) Keep client-side error handling independent from raw HTTP text.
 * </pre>
 */
public enum ErrorCode {
    /** Request is invalid or malformed. */
    BAD_REQUEST,
    /** Missing authentication. */
    UNAUTHORIZED,
    /** Insufficient permissions. */
    FORBIDDEN,
    /** Requested resource does not exist. */
    NOT_FOUND,
    /** Conflict with current resource state. */
    CONFLICT,
    /** Validation failed for request data. */
    VALIDATION_ERROR,
    /** Downstream dependency failed. */
    DEPENDENCY_ERROR,
    /** Unexpected server error. */
    INTERNAL_ERROR
}
