package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for trace id resolution. */
class AccessLogFilterTraceIdTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Trace header name used by tests. */
    private static final String TRACE_HEADER = "X-Request-Id";

    /** Trace header value used by tests. */
    private static final String TRACE_VALUE = "trace-123";

    /** HTTP GET method name. */
    private static final String METHOD_GET = "GET";

    /**
     * Existing trace header is returned.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: Existing trace header is returned
     * Test conditions: X-Request-Id header is present
     * Test result: Same value is returned
     * </pre>
     */
    @Test
    void resolveTraceIdUsesHeader() {
        final MockHttpServletRequest withTrace = new MockHttpServletRequest(METHOD_GET, PATH_API);
        withTrace.addHeader(TRACE_HEADER, TRACE_VALUE);

        Assertions.assertEquals(TRACE_VALUE, AccessLogFilter.resolveTraceId(withTrace),
                "Trace id header should be reused");
    }

    /**
     * Missing trace id is generated.
     *
     * <pre>
     * Theme: Trace propagation
     * Test view: Missing trace id is generated
     * Test conditions: No trace header
     * Test result: Generated trace id is not null
     * </pre>
     */
    @Test
    void resolveTraceIdGeneratesWhenMissing() {
        final MockHttpServletRequest withoutTrace =
                new MockHttpServletRequest(METHOD_GET, PATH_API);

        Assertions.assertNotNull(AccessLogFilter.resolveTraceId(withoutTrace),
                "Trace id should be generated when missing");
    }
}
