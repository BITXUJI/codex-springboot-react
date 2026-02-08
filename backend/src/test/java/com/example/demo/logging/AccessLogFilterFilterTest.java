package com.example.demo.logging;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Tests for access-log filter execution behavior. */
class AccessLogFilterFilterTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Trace header name used by tests. */
    private static final String TRACE_HEADER = "X-Request-Id";

    /** Trace header value used by tests. */
    private static final String TRACE_VALUE = "trace-123";

    /** JSON content type used by tests. */
    private static final String CONTENT_TYPE_JSON = "application/json";

    /** Text content type used by tests. */
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    /** Response body used by tests. */
    private static final String RESPONSE_BODY = "ok";

    /** HTTP GET method name. */
    private static final String METHOD_GET = "GET";

    /** HTTP POST method name. */
    private static final String METHOD_POST = "POST";

    /** Clears MDC values after each test. */
    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * Incoming trace header is reused.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: Incoming trace header is reused
     * Test conditions: Request includes X-Request-Id
     * Test result: Response contains the same trace id
     * </pre>
     */
    @Test
    void doFilterInternalPropagatesTraceIdHeader() throws IOException, ServletException {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_POST, PATH_API);
        request.setContentType(CONTENT_TYPE_JSON);
        request.setContent("{\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(TRACE_HEADER, TRACE_VALUE);

        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            req.getInputStream().readAllBytes();
            res.setContentType(CONTENT_TYPE_TEXT);
            res.getOutputStream().write(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
        });

        Assertions.assertEquals(TRACE_VALUE, response.getHeader(TRACE_HEADER),
                "Response should echo the incoming trace id");
    }

    /**
     * Cached response body is copied back.
     *
     * <pre>
     * Theme: Response buffering
     * Test view: Cached response body is copied back
     * Test conditions: Response writes body content
     * Test result: Client receives the expected body
     * </pre>
     */
    @Test
    void doFilterInternalCopiesResponseBody() throws IOException, ServletException {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_POST, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> res.getOutputStream()
                .write(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals(RESPONSE_BODY, response.getContentAsString(),
                "Response body should be preserved");
    }

    /**
     * Missing trace header is generated.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: Missing trace header is generated
     * Test conditions: Request without X-Request-Id
     * Test result: Response has a generated trace id
     * </pre>
     */
    @Test
    void doFilterInternalGeneratesTraceIdWhenMissing() throws IOException, ServletException {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> res.getOutputStream()
                .write(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(response.getHeader(TRACE_HEADER),
                "Trace id should be generated when missing");
    }

    /**
     * IO failures are rethrown.
     *
     * <pre>
     * Theme: Error propagation
     * Test view: IO failures are rethrown
     * Test conditions: Filter chain throws IOException
     * Test result: IOException is propagated
     * </pre>
     */
    @Test
    void doFilterInternalHandlesIoException() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertThrows(IOException.class,
                () -> filter.doFilterInternal(request, response, (req, res) -> {
                    throw new IOException("io-failure");
                }), "IOException should bubble up");
    }

    /**
     * Runtime failures are wrapped as servlet exceptions.
     *
     * <pre>
     * Theme: Error propagation
     * Test view: Runtime failures are wrapped as servlet exceptions
     * Test conditions: Filter chain throws runtime exception
     * Test result: ServletException is propagated with runtime cause
     * </pre>
     */
    @Test
    void doFilterInternalHandlesRuntimeException() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final ServletException thrown = Assertions.assertThrows(ServletException.class,
                () -> filter.doFilterInternal(request, response, (req, res) -> {
                    throw new IllegalStateException("boom");
                }), "Runtime exceptions should be wrapped as ServletException");
        Assertions.assertInstanceOf(IllegalStateException.class, thrown.getCause(),
                "ServletException should preserve runtime cause");
    }

    /**
     * Runtime failures are passed to access logging.
     *
     * <pre>
     * Theme: Error propagation
     * Test view: Runtime failures are passed to access logging
     * Test conditions: Filter chain throws runtime exception
     * Test result: logAccess receives the runtime failure instance
     * </pre>
     */
    @Test
    void doFilterInternalCapturesRuntimeFailureForLogging() {
        final CapturingAccessLogFilter filter = new CapturingAccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertThrows(ServletException.class,
                () -> filter.doFilterInternal(request, response, (req, res) -> {
                    throw new IllegalStateException("boom");
                }), "Runtime exceptions should be wrapped as servlet exceptions");
        Assertions.assertInstanceOf(IllegalStateException.class, filter.capturedFailure,
                "Runtime failure type should be preserved for logging");
    }

    /**
     * Existing MDC values are restored after filtering.
     *
     * <pre>
     * Theme: MDC context management
     * Test view: Existing MDC values are restored after filtering
     * Test conditions: MDC has pre-existing keys before filter execution
     * Test result: Existing keys remain after filter cleanup
     * </pre>
     */
    @Test
    void doFilterInternalRestoresExistingMdcContext() throws IOException, ServletException {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("userId", "u-1");
        MDC.put("traceId", "old-trace");

        filter.doFilterInternal(request, response, (req, res) -> res.getOutputStream()
                .write(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals("u-1", MDC.get("userId"),
                "Existing MDC userId should be preserved");
        Assertions.assertEquals("old-trace", MDC.get("traceId"),
                "Existing MDC traceId should be restored");
    }

    /**
     * Test helper that captures failures passed to logAccess.
     *
     * <pre>
     * Responsibilities:
     * 1) Override logAccess to observe the failure argument.
     * 2) Avoid changing production behavior in tested code paths.
     * </pre>
     */
    private static final class CapturingAccessLogFilter extends AccessLogFilter {
        /** Captured failure from logAccess. */
        private Exception capturedFailure;

        @Override
        protected void logAccess(final ContentCachingRequestWrapper request,
                final ContentCachingResponseWrapper response, final long durationMs,
                final Exception failure) {
            capturedFailure = failure;
        }
    }
}
