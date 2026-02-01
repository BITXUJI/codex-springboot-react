package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link LogSanitizer} header masking. */
class LogSanitizerHeaderTest {
    /** Authorization header name used by tests. */
    private static final String HEADER_AUTH = "Authorization";

    /** API key header name used by tests. */
    private static final String HEADER_API_KEY = "X-Api-Key";

    /** Custom header name used by tests. */
    private static final String HEADER_OTHER = "X-Other";

    /** Placeholder value used by tests. */
    private static final String VALUE_TOKEN = "abc";

    /** Placeholder value used by tests. */
    private static final String VALUE_TEXT = "value";

    /**
     * Authorization header is masked.
     *
     * <pre>
     * Theme: Header masking
     * Test view: Authorization header is masked
     * Test conditions: Authorization header with value
     * Test result: Masked value is returned
     * </pre>
     */
    @Test
    void maskHeaderValueMasksAuthorization() {
        Assertions.assertEquals("****",
                LogSanitizer.maskHeaderValue(HEADER_AUTH, "Bearer " + VALUE_TOKEN),
                "Authorization header should be masked");
    }

    /**
     * API key header is masked.
     *
     * <pre>
     * Theme: Header masking
     * Test view: API key header is masked
     * Test conditions: X-Api-Key header with value
     * Test result: Masked value is returned
     * </pre>
     */
    @Test
    void maskHeaderValueMasksApiKey() {
        Assertions.assertEquals("****", LogSanitizer.maskHeaderValue(HEADER_API_KEY, VALUE_TOKEN),
                "API key header should be masked");
    }

    /**
     * Non-sensitive headers are unchanged.
     *
     * <pre>
     * Theme: Header masking
     * Test view: Non-sensitive headers are unchanged
     * Test conditions: Custom header value
     * Test result: Original value is returned
     * </pre>
     */
    @Test
    void maskHeaderValueLeavesOtherHeaders() {
        Assertions.assertEquals(VALUE_TEXT, LogSanitizer.maskHeaderValue(HEADER_OTHER, VALUE_TEXT),
                "Non-sensitive headers should not be masked");
    }

    /**
     * Null header names are allowed.
     *
     * <pre>
     * Theme: Header masking
     * Test view: Null header names are allowed
     * Test conditions: Null header name with value
     * Test result: Original value is returned
     * </pre>
     */
    @Test
    void maskHeaderValueHandlesNullHeaderName() {
        Assertions.assertEquals(VALUE_TEXT, LogSanitizer.maskHeaderValue(null, VALUE_TEXT),
                "Null header names should return original value");
    }

    /**
     * Null header values are preserved.
     *
     * <pre>
     * Theme: Header masking
     * Test view: Null header values are preserved
     * Test conditions: Header value is null
     * Test result: Null is returned
     * </pre>
     */
    @Test
    void maskHeaderValueHandlesNullHeaderValue() {
        Assertions.assertNull(LogSanitizer.maskHeaderValue(HEADER_AUTH, null),
                "Null header values should remain null");
    }
}
