package com.example.demo.error;

/** Standardized error codes for API responses. */
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
