package com.example.demo.error;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Maps exceptions to the API's standard error payload.
 * 
 * <pre>
 * Responsibilities:
 * 1) Convert framework and domain exceptions into uniform ErrorResponse bodies.
 * 2) Preserve useful validation details for client troubleshooting.
 * 3) Keep status-to-error-code mapping consistent across handlers.
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Handles domain exceptions with an explicit status.
     * 
     * <pre>
     * Algorithm:
     * 1) Read status, code, message, and details from AppException.
     * 2) Delegate payload construction to ErrorResponseFactory.
     * 3) Return the factory-built response entity unchanged.
     * </pre>
     *
     * @param exception application exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(final AppException exception,
            final HttpServletRequest request) {
        return ErrorResponseFactory.buildResponse(exception.getStatus(), exception.getCode().name(),
                exception.getMessage(), exception.getDetails(), request);
    }

    /**
     * Handles bean validation failures for request bodies.
     * 
     * <pre>
     * Algorithm:
     * 1) Iterate Spring FieldError entries and map them to API FieldError objects.
     * 2) Attach mapped field errors when the list is not empty.
     * 3) Return BAD_REQUEST with VALIDATION_ERROR code.
     * </pre>
     *
     * @param exception validation exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            final MethodArgumentNotValidException exception, final HttpServletRequest request) {
        final ErrorDetails details = new ErrorDetails();
        final List<com.example.demo.model.FieldError> fieldErrors = new ArrayList<>();
        for (final FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.add(ErrorResponseFactory.toFieldError(error.getField(),
                    error.getDefaultMessage(), error.getRejectedValue()));
        }
        if (!fieldErrors.isEmpty()) {
            details.setFieldErrors(fieldErrors);
        }
        return ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR.name(), "Validation failed", details, request);
    }

    /**
     * Handles constraint violations for request parameters.
     * 
     * <pre>
     * Algorithm:
     * 1) Iterate constraint violations and extract path, message, and invalid value.
     * 2) Convert each item into API FieldError entries.
     * 3) Return BAD_REQUEST with VALIDATION_ERROR code and optional details.
     * </pre>
     *
     * @param exception constraint violation exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            final ConstraintViolationException exception, final HttpServletRequest request) {
        final ErrorDetails details = new ErrorDetails();
        final List<com.example.demo.model.FieldError> fieldErrors = new ArrayList<>();
        for (final ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            final String field = java.util.Objects.toString(violation.getPropertyPath(), null);
            fieldErrors.add(ErrorResponseFactory.toFieldError(field, violation.getMessage(),
                    violation.getInvalidValue()));
        }
        if (!fieldErrors.isEmpty()) {
            details.setFieldErrors(fieldErrors);
        }
        return ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR.name(), "Validation failed", details, request);
    }

    /**
     * Handles unreadable or malformed request bodies.
     * 
     * <pre>
     * Algorithm:
     * 1) Treat parsing/deserialize failures as client input errors.
     * 2) Return BAD_REQUEST with a fixed "Malformed request body" message.
     * 3) Omit details because field-level mapping is not reliable here.
     * </pre>
     *
     * @param exception message not readable exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            final HttpMessageNotReadableException exception, final HttpServletRequest request) {
        return ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST.name(), "Malformed request body", null, request);
    }

    /**
     * Handles Spring response status exceptions.
     * 
     * <pre>
     * Algorithm:
     * 1) Convert status code value to HttpStatus.
     * 2) Map status to API error code with ErrorResponseFactory.
     * 3) Use explicit reason when present.
     * 4) Fallback to the default reason phrase when reason is missing.
     * </pre>
     *
     * @param exception response status exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            final ResponseStatusException exception, final HttpServletRequest request) {
        final HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        final ErrorCode code = ErrorResponseFactory.mapStatusToCode(status);
        final String message =
                exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ErrorResponseFactory.buildResponse(status, code.name(), message, null, request);
    }

    /**
     * Handles unmatched routes when configured to throw.
     * 
     * <pre>
     * Algorithm:
     * 1) Treat unresolved routes as NOT_FOUND.
     * 2) Use a stable "Not found" message and NOT_FOUND code.
     * 3) Return the standardized error payload.
     * </pre>
     *
     * @param exception no handler exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final NoHandlerFoundException exception,
            final HttpServletRequest request) {
        return ErrorResponseFactory.buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.name(),
                "Not found", null, request);
    }

    /**
     * Handles unexpected errors with a generic response.
     * 
     * <pre>
     * Algorithm:
     * 1) Catch any uncategorized Exception as server failure.
     * 2) Return INTERNAL_SERVER_ERROR with INTERNAL_ERROR code.
     * 3) Use a generic message to avoid leaking internals.
     * </pre>
     *
     * @param exception unhandled exception
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(final Exception exception,
            final HttpServletRequest request) {
        return ErrorResponseFactory.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.name(), "Unexpected error", null, request);
    }
}
