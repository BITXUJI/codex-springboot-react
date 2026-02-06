package com.example.demo.error;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Builds standard error responses for the API.
 * 
 * <pre>
 * Responsibilities:
 * 1) Assemble canonical ErrorResponse payloads.
 * 2) Resolve trace identifiers consistently across request contexts.
 * 3) Convert validation and status data to API-facing error structures.
 * </pre>
 */
public final class ErrorResponseFactory {
    /** Request/response header used for trace propagation. */
    private static final String TRACE_ID_HEADER = "X-Request-Id";

    /**
     * Utility class; no instances.
     * 
     * <pre>
     * Design note:
     * 1) All behavior is exposed as static helper methods.
     * 2) Private constructor prevents accidental instantiation.
     * </pre>
     */
    private ErrorResponseFactory() {}

    /**
     * Builds a standard error response payload.
     * 
     * <pre>
     * Algorithm:
     * 1) Create ErrorResponse and set code, message, traceId, timestamp, and path.
     * 2) Attach details only when field errors or additional properties are present.
     * 3) Return a ResponseEntity with the provided HTTP status.
     * </pre>
     *
     * @param status HTTP status
     * @param code error code
     * @param message error message
     * @param details optional details
     * @param request current request
     * @return response entity
     */
    public static ResponseEntity<ErrorResponse> buildResponse(final HttpStatus status,
            final String code, final String message, final ErrorDetails details,
            final HttpServletRequest request) {
        final ErrorResponse body = new ErrorResponse();
        body.setCode(code);
        body.setMessage(message);
        body.setTraceId(resolveTraceId(request));
        body.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        body.setPath(request.getRequestURI());
        if (details != null && details.getFieldErrors() != null
                && !details.getFieldErrors().isEmpty()) {
            body.setDetails(details);
        } else if (details != null && details.getAdditionalProperties() != null
                && !details.getAdditionalProperties().isEmpty()) {
            body.setDetails(details);
        }
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Resolves the current trace identifier.
     * 
     * <pre>
     * Algorithm:
     * 1) Read traceId from MDC when available.
     * 2) Fallback to X-Request-Id request header.
     * 3) Generate a random UUID when no incoming value exists.
     * </pre>
     *
     * @param request current request
     * @return trace id
     */
    public static String resolveTraceId(final HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(TRACE_ID_HEADER);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        return traceId;
    }

    /**
     * Maps validation data into the error field payload.
     * 
     * <pre>
     * Algorithm:
     * 1) Create a FieldError model instance.
     * 2) Populate field and message (fallback to "Invalid value" when missing).
     * 3) Store rejected value as JsonNullable.undefined() or stringified value.
     * </pre>
     *
     * @param field field name
     * @param message validation message
     * @param rejectedValue invalid value
     * @return field error payload
     */
    public static com.example.demo.model.FieldError toFieldError(final String field,
            final String message, final Object rejectedValue) {
        final com.example.demo.model.FieldError error = new com.example.demo.model.FieldError();
        error.setField(field);
        error.setMessage(message == null ? "Invalid value" : message);
        if (rejectedValue == null) {
            error.setRejectedValue(org.openapitools.jackson.nullable.JsonNullable.undefined());
        } else {
            error.setRejectedValue(org.openapitools.jackson.nullable.JsonNullable
                    .of(String.valueOf(rejectedValue)));
        }
        return error;
    }

    /**
     * Maps HTTP status codes to error codes.
     * 
     * <pre>
     * Algorithm:
     * 1) Switch on the provided HttpStatus.
     * 2) Map common security and client statuses explicitly.
     * 3) Use INTERNAL_ERROR as the default fallback.
     * </pre>
     *
     * @param status HTTP status
     * @return error code
     */
    public static ErrorCode mapStatusToCode(final HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
