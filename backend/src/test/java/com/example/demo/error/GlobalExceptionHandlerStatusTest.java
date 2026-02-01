package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

/** Tests for {@link GlobalExceptionHandler} status mappings. */
class GlobalExceptionHandlerStatusTest {
    /** Trace id used for test requests. */
    private static final String TRACE_ID = "trace-abc";

    /**
     * Application exceptions map to configured status.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Application exceptions map to configured status
     * Test conditions: AppException with BAD_REQUEST
     * Test result: Response status and code reflect the exception
     * </pre>
     */
    @Test
    void handleAppExceptionReturnsConfiguredStatus() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");
        request.addHeader("X-Request-Id", TRACE_ID);

        final ErrorDetails details = new ErrorDetails();
        details.putAdditionalProperty("info", "value");
        final AppException exception = new AppException(ErrorCode.BAD_REQUEST, "Bad request",
                HttpStatus.BAD_REQUEST, details);

        final ResponseEntity<ErrorResponse> response =
                handler.handleAppException(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.name());
    }

    /**
     * Malformed payloads return bad request.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Malformed payloads return bad request
     * Test conditions: HttpMessageNotReadableException thrown
     * Test result: Response status is BAD_REQUEST
     * </pre>
     */
    @Test
    void handleNotReadableReturnsBadRequest() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleNotReadable(
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Response status exceptions map to codes.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Response status exceptions map to codes
     * Test conditions: ResponseStatusException with NOT_FOUND
     * Test result: Response status and code match NOT_FOUND
     * </pre>
     */
    @Test
    void handleResponseStatusReturnsMappedCode() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing"), request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    /**
     * Missing reason uses default phrase.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Missing reason uses default phrase
     * Test conditions: ResponseStatusException without reason
     * Test result: Message equals the default reason phrase
     * </pre>
     */
    @Test
    void handleResponseStatusUsesDefaultReason() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler
                .handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND), request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    /**
     * Not found handler returns 404.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Not found handler returns 404
     * Test conditions: NoHandlerFoundException is raised
     * Test result: Response status is NOT_FOUND
     * </pre>
     */
    @Test
    void handleNotFoundReturnsNotFound() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/missing");

        final NoHandlerFoundException exception =
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders());
        final ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Unhandled exceptions map to internal errors.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Unhandled exceptions map to internal errors
     * Test conditions: Runtime exception is thrown
     * Test result: Response status is INTERNAL_SERVER_ERROR
     * </pre>
     */
    @Test
    void handleUnhandledReturnsInternalServerError() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response =
                handler.handleUnhandled(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
