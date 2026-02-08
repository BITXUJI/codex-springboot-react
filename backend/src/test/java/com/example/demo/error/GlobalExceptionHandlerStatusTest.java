package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
     * Unknown 4xx status falls back safely.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Unknown 4xx status falls back safely
     * Test conditions: ResponseStatusException uses custom 499 status code
     * Test result: Response uses BAD_REQUEST with BAD_REQUEST code
     * </pre>
     */
    @Test
    void handleResponseStatusHandlesUnknown4xxStatusCode() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatusCode.valueOf(499), "Client Closed Request"),
                request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.name());
        assertThat(body.getMessage()).isEqualTo("Client Closed Request");
    }

    /**
     * Unknown 5xx status falls back to internal error.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Unknown 5xx status falls back to internal error
     * Test conditions: ResponseStatusException uses custom 599 status code
     * Test result: Response uses INTERNAL_SERVER_ERROR with INTERNAL_ERROR code
     * </pre>
     */
    @Test
    void handleResponseStatusHandlesUnknown5xxStatusCode() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatusCode.valueOf(599)), request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        assertThat(body.getMessage()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
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
