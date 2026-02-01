package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for {@link ErrorResponseFactory} response building. */
class ErrorResponseFactoryBuildResponseTest {
    /** API path used in build response tests. */
    private static final String API_PATH = "/api/hello";
    /** Trace id used in build response tests. */
    private static final String TRACE_ID = "trace-123";
    /** Sample field name used in tests. */
    private static final String FIELD_NAME = "name";
    /** Sample validation message. */
    private static final String VALIDATION_MSG = "Validation failed";
    /** Sample request error message. */
    private static final String BAD_REQUEST_MSG = "Bad request";
    /** Sample error detail message. */
    private static final String FIELD_MESSAGE = "must not be blank";
    /** Sample additional property key. */
    private static final String INFO_KEY = "info";
    /** Sample additional property value. */
    private static final String INFO_VALUE = "extra";

    /** Clears MDC between tests. */
    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * Field errors are preserved.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Field errors are preserved
     * Test conditions: Details include field errors
     * Test result: Response contains the field errors and trace id
     * </pre>
     */
    @Test
    void buildResponseIncludesFieldErrors() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);
        request.addHeader("X-Request-Id", TRACE_ID);

        final ErrorDetails details = new ErrorDetails();
        details.setFieldErrors(
                List.of(ErrorResponseFactory.toFieldError(FIELD_NAME, FIELD_MESSAGE, null)));

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_ERROR.name(), VALIDATION_MSG, details, request);

        final ErrorResponse body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getTraceId()).isEqualTo(TRACE_ID);

        final ErrorDetails responseDetails = body.getDetails();
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getFieldErrors()).hasSize(1);
    }

    /**
     * Additional properties are preserved.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Additional properties are preserved
     * Test conditions: Details include extra properties
     * Test result: Response contains the extra properties
     * </pre>
     */
    @Test
    void buildResponseIncludesAdditionalProperties() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);

        final ErrorDetails details = new ErrorDetails();
        details.putAdditionalProperty(INFO_KEY, INFO_VALUE);

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.name(), BAD_REQUEST_MSG, details, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();

        final ErrorDetails responseDetails = body.getDetails();
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getAdditionalProperties()).containsEntry(INFO_KEY, INFO_VALUE);
    }

    /**
     * Empty details are omitted.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Empty details are omitted
     * Test conditions: Details object is empty
     * Test result: Response details are null
     * </pre>
     */
    @Test
    void buildResponseOmitsEmptyDetails() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.name(), BAD_REQUEST_MSG, new ErrorDetails(), request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }

    /**
     * Null details are handled.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Null details are handled
     * Test conditions: Details is null
     * Test result: Response details are null
     * </pre>
     */
    @Test
    void buildResponseHandlesNullDetails() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_ERROR.name(), "Unexpected error", null, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }

    /**
     * Null field errors are handled.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Null field errors are handled
     * Test conditions: Details have null field errors
     * Test result: Response details are null
     * </pre>
     */
    @Test
    void buildResponseHandlesNullFieldErrors() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);

        final ErrorDetails details = new ErrorDetails();
        details.setFieldErrors(null);

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.name(), BAD_REQUEST_MSG, details, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }

    /**
     * Empty additional properties are ignored.
     *
     * <pre>
     * Theme: Error response building
     * Test view: Empty additional properties are ignored
     * Test conditions: Additional properties are cleared
     * Test result: Response details are null
     * </pre>
     */
    @Test
    void buildResponseIgnoresEmptyAdditionalProperties() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(API_PATH);

        final ErrorDetails details = new ErrorDetails();
        details.putAdditionalProperty("temp", "value");
        details.getAdditionalProperties().clear();

        final ResponseEntity<ErrorResponse> response =
                ErrorResponseFactory.buildResponse(HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST.name(), BAD_REQUEST_MSG, details, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }

    /**
     * MDC trace id takes priority.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: MDC trace id takes priority
     * Test conditions: MDC trace id is present
     * Test result: Response uses the MDC trace id
     * </pre>
     */
    @Test
    void resolveTraceIdUsesMdcWhenPresent() {
        MDC.put("traceId", "mdc-trace");
        final HttpServletRequest request = new MockHttpServletRequest();

        assertThat(ErrorResponseFactory.resolveTraceId(request)).isEqualTo("mdc-trace");
    }

    /**
     * Missing trace id is generated.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: Missing trace id is generated
     * Test conditions: No trace id in MDC or headers
     * Test result: Generated trace id is not blank
     * </pre>
     */
    @Test
    void resolveTraceIdGeneratesWhenMissing() {
        final HttpServletRequest request = new MockHttpServletRequest();

        assertThat(ErrorResponseFactory.resolveTraceId(request)).isNotBlank();
    }
}
