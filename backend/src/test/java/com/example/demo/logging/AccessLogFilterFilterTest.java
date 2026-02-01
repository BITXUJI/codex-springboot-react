package com.example.demo.logging;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
     * Runtime failures are rethrown.
     *
     * <pre>
     * Theme: Error propagation
     * Test view: Runtime failures are rethrown
     * Test conditions: Filter chain throws runtime exception
     * Test result: Runtime exception is propagated
     * </pre>
     */
    @Test
    void doFilterInternalHandlesRuntimeException() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        Assertions.assertThrows(IllegalStateException.class,
                () -> filter.doFilterInternal(request, response, (req, res) -> {
                    throw new IllegalStateException("boom");
                }), "Runtime exceptions should bubble up");
    }
}
