package com.example.demo.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Tests for access log emission behavior. */
class AccessLogFilterLogAccessTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Logger name used by AccessLogFilter. */
    private static final String ACCESS_LOGGER = "ACCESS_LOG";

    /** Body capture size used by tests. */
    private static final int BODY_LIMIT = 4096;

    /** HTTP GET method name. */
    private static final String METHOD_GET = "GET";

    /** Clears MDC context after each test to avoid cross-test leakage. */
    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * Disabled logger does not throw.
     *
     * <pre>
     * Theme: Access logging
     * Test view: Disabled logger does not throw
     * Test conditions: ACCESS_LOG level is OFF
     * Test result: logAccess completes without error
     * </pre>
     */
    @Test
    void logAccessDoesNotThrowWhenDisabled() {
        final AccessLogFilter filter = new AccessLogFilter();
        final Logger accessLogger = (Logger) LoggerFactory.getLogger(ACCESS_LOGGER);
        final Level previousLevel = accessLogger.getLevel();

        final ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(
                new MockHttpServletRequest(METHOD_GET, PATH_API), BODY_LIMIT);
        final ContentCachingResponseWrapper resWrapper =
                new ContentCachingResponseWrapper(new MockHttpServletResponse());

        accessLogger.setLevel(Level.OFF);
        Assertions.assertDoesNotThrow(() -> filter.logAccess(reqWrapper, resWrapper, 1L, null),
                "logAccess should not throw when logger is disabled");
        accessLogger.setLevel(previousLevel);
    }

    /**
     * Failure with default status does not throw.
     *
     * <pre>
     * Theme: Access logging
     * Test view: Failure with default status does not throw
     * Test conditions: Exception provided and status is < 400
     * Test result: logAccess completes without error
     * </pre>
     */
    @Test
    void logAccessDoesNotThrowWithFailure() {
        final AccessLogFilter filter = new AccessLogFilter();
        final Logger accessLogger = (Logger) LoggerFactory.getLogger(ACCESS_LOGGER);
        final Level previousLevel = accessLogger.getLevel();

        final ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(
                new MockHttpServletRequest(METHOD_GET, PATH_API), BODY_LIMIT);
        final ContentCachingResponseWrapper resWrapper =
                new ContentCachingResponseWrapper(new MockHttpServletResponse());

        accessLogger.setLevel(Level.INFO);
        Assertions.assertDoesNotThrow(
                () -> filter.logAccess(reqWrapper, resWrapper, 1L, new IOException("failure")),
                "logAccess should not throw with failure and default status");
        accessLogger.setLevel(previousLevel);
    }

    /**
     * Failure with server status does not throw.
     *
     * <pre>
     * Theme: Access logging
     * Test view: Failure with server status does not throw
     * Test conditions: Exception provided and status already 500
     * Test result: logAccess completes without error
     * </pre>
     */
    @Test
    void logAccessDoesNotThrowWithServerErrorStatus() {
        final AccessLogFilter filter = new AccessLogFilter();
        final Logger accessLogger = (Logger) LoggerFactory.getLogger(ACCESS_LOGGER);
        final Level previousLevel = accessLogger.getLevel();

        final ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(
                new MockHttpServletRequest(METHOD_GET, PATH_API), BODY_LIMIT);
        final MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        errorResponse.setStatus(500);
        final ContentCachingResponseWrapper resWrapper =
                new ContentCachingResponseWrapper(errorResponse);

        accessLogger.setLevel(Level.INFO);
        Assertions.assertDoesNotThrow(
                () -> filter.logAccess(reqWrapper, resWrapper, 1L, new IOException("failure")),
                "logAccess should not throw when status is server error");
        accessLogger.setLevel(previousLevel);
    }

    /**
     * Trace id relies on MDC and avoids duplicated structured field.
     *
     * <pre>
     * Theme: Access logging
     * Test view: Trace id relies on MDC and avoids duplicated structured field
     * Test conditions: MDC contains trace id and log event is emitted
     * Test result: Structured key-value pairs do not include explicit traceId entry
     * </pre>
     */
    @Test
    void logAccessDoesNotAddTraceIdKeyValuePair() {
        final AccessLogFilter filter = new AccessLogFilter();
        final Logger accessLogger = (Logger) LoggerFactory.getLogger(ACCESS_LOGGER);
        final Level previousLevel = accessLogger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);

        final ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(
                new MockHttpServletRequest(METHOD_GET, PATH_API), BODY_LIMIT);
        final ContentCachingResponseWrapper resWrapper =
                new ContentCachingResponseWrapper(new MockHttpServletResponse());
        try {
            MDC.put("traceId", "trace-abc");
            accessLogger.setLevel(Level.INFO);
            filter.logAccess(reqWrapper, resWrapper, 1L, null);

            Assertions.assertFalse(appender.list.isEmpty(), "A log event should be emitted");
            final ILoggingEvent event = appender.list.get(0);
            final long tracePairs = event.getKeyValuePairs() == null ? 0L
                    : event.getKeyValuePairs().stream().filter(pair -> "traceId".equals(pair.key))
                            .count();
            Assertions.assertEquals(0L, tracePairs,
                    "traceId should come from MDC, not duplicate key-value arguments");
        } finally {
            accessLogger.detachAppender(appender);
            appender.stop();
            accessLogger.setLevel(previousLevel);
        }
    }

}
