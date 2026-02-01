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

/** Maps exceptions to the API's standard error payload. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Handles domain exceptions with an explicit status.
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
