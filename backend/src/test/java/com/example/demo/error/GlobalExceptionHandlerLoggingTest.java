package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for {@link GlobalExceptionHandler} logging behavior. */
class GlobalExceptionHandlerLoggingTest {
    /** Logger used by GlobalExceptionHandler. */
    private static final String HANDLER_LOGGER = GlobalExceptionHandler.class.getName();

    /**
     * Unhandled exceptions are emitted as error logs.
     *
     * <pre>
     * Theme: Exception logging
     * Test view: Unhandled exceptions are emitted as error logs
     * Test conditions: Generic exception is handled
     * Test result: Error log entry is produced with attached throwable
     * </pre>
     */
    @Test
    void handleUnhandledEmitsErrorLog() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");
        request.addHeader("X-Request-Id", "trace-abc");

        final Logger logger = (Logger) LoggerFactory.getLogger(HANDLER_LOGGER);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            handler.handleUnhandled(new IllegalStateException("boom"), request);
            assertThat(appender.list).isNotEmpty();
            final ILoggingEvent event = appender.list.get(0);
            assertThat(event.getLevel().toString()).isEqualTo("ERROR");
            assertThat(event.getThrowableProxy()).isNotNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    /**
     * Error mapping still works when logger level disables error logging.
     *
     * <pre>
     * Theme: Exception logging
     * Test view: Error mapping still works when logger level disables error logging
     * Test conditions: Handler logger level is OFF
     * Test result: Response remains INTERNAL_SERVER_ERROR
     * </pre>
     */
    @Test
    void handleUnhandledWhenErrorLoggingDisabled() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final Logger logger = (Logger) LoggerFactory.getLogger(HANDLER_LOGGER);
        final Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            final ResponseEntity<ErrorResponse> response =
                    handler.handleUnhandled(new IllegalStateException("boom"), request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            logger.setLevel(previousLevel);
        }
    }
}
