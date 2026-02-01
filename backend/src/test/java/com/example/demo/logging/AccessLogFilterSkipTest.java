package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for access-log skip path logic. */
class AccessLogFilterSkipTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Actuator path used by tests. */
    private static final String PATH_ACTUATOR = "/actuator/health";

    /** Health path used by tests. */
    private static final String PATH_HEALTH = "/health";

    /** HTTP GET method name. */
    private static final String METHOD_GET = "GET";

    /**
     * Skip paths for actuator.
     *
     * <pre>
     * Theme: Access log skipping
     * Test view: Skip paths for actuator
     * Test conditions: Request URI under /actuator
     * Test result: Filter is skipped
     * </pre>
     */
    @Test
    void shouldSkipActuatorPath() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request =
                new MockHttpServletRequest(METHOD_GET, PATH_ACTUATOR);

        Assertions.assertTrue(filter.shouldNotFilter(request),
                "Actuator requests should be skipped");
    }

    /**
     * Skip paths for health.
     *
     * <pre>
     * Theme: Access log skipping
     * Test view: Skip paths for health
     * Test conditions: Request URI is /health
     * Test result: Filter is skipped
     * </pre>
     */
    @Test
    void shouldSkipHealthPath() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HEALTH);

        Assertions.assertTrue(filter.shouldNotFilter(request), "Health requests should be skipped");
    }

    /**
     * Non-skipped API paths.
     *
     * <pre>
     * Theme: Access log skipping
     * Test view: Non-skipped API paths
     * Test conditions: Request URI is /api/hello
     * Test result: Filter is applied
     * </pre>
     */
    @Test
    void shouldNotSkipApiPath() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);

        Assertions.assertFalse(filter.shouldNotFilter(request),
                "API requests should not be skipped");
    }

    /**
     * Blank path handling.
     *
     * <pre>
     * Theme: Access log skipping
     * Test view: Blank path handling
     * Test conditions: Request URI is empty
     * Test result: Filter is applied
     * </pre>
     */
    @Test
    void shouldNotSkipBlankPath() {
        final AccessLogFilter filter = new AccessLogFilter();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("");

        Assertions.assertFalse(filter.shouldNotFilter(request),
                "Blank paths should not be skipped");
    }
}
