package com.example.demo.error;

import com.example.demo.model.ErrorDetails;
import java.util.ArrayList;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain-friendly exception with structured error metadata.
 * 
 * <pre>
 * Responsibilities:
 * 1) Store API-facing error code and HTTP status together with the message.
 * 2) Optionally carry structured ErrorDetails for clients.
 * 3) Protect internal state by copying mutable detail data.
 * </pre>
 */
@Getter
public class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Error code for the response. */
    private final ErrorCode code;
    /** HTTP status for the response. */
    private final HttpStatus status;
    /** Optional structured error details. */
    private final transient ErrorDetails details;

    /**
     * Creates a domain exception with a status and message.
     * 
     * <pre>
     * Algorithm:
     * 1) Delegate to the full constructor.
     * 2) Pass null for optional structured details.
     * </pre>
     *
     * @param code error code
     * @param message error message
     * @param status HTTP status
     */
    public AppException(final ErrorCode code, final String message, final HttpStatus status) {
        this(code, message, status, null);
    }

    /**
     * Creates a domain exception with optional details.
     * 
     * <pre>
     * Algorithm:
     * 1) Store the message in RuntimeException via super(message).
     * 2) Save code and status exactly as provided.
     * 3) Copy details defensively to avoid external mutation.
     * </pre>
     *
     * @param code error code
     * @param message error message
     * @param status HTTP status
     * @param details structured error details
     */
    public AppException(final ErrorCode code, final String message, final HttpStatus status,
            final ErrorDetails details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = copyDetails(details);
    }

    /**
     * Returns structured error details.
     * 
     * <pre>
     * Algorithm:
     * 1) Read the internally stored details reference.
     * 2) Return a defensive copy for caller safety.
     * 3) Return null when no details exist.
     * </pre>
     *
     * @return details or null
     */
    public ErrorDetails getDetails() {
        return copyDetails(details);
    }

    /**
     * Copies the error details to avoid exposing internal state.
     * 
     * <pre>
     * Algorithm:
     * 1) Return null when source is null.
     * 2) Create a new ErrorDetails instance.
     * 3) Copy fieldErrors into a new list when present.
     * 4) Copy additionalProperties entries one by one.
     * 5) Return the copied object.
     * </pre>
     *
     * @param source original details
     * @return defensive copy or null
     */
    private static ErrorDetails copyDetails(final ErrorDetails source) {
        ErrorDetails copy = null;
        if (source != null) {
            copy = new ErrorDetails();
            if (source.getFieldErrors() != null) {
                copy.setFieldErrors(new ArrayList<>(source.getFieldErrors()));
            }
            final Map<String, Object> extraProperties = source.getAdditionalProperties();
            if (extraProperties != null) {
                for (final Map.Entry<String, Object> entry : extraProperties.entrySet()) {
                    copy.putAdditionalProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        return copy;
    }
}
