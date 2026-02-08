package com.example.demo.logging;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for charset resolution logic. */
class AccessLogFilterCharsetTest {
    /** Text content type used by tests. */
    private static final String CT_TEXT = "text/plain";

    /**
     * Null content types fallback to UTF-8.
     *
     * <pre>
     * Theme: Charset resolution
     * Test view: Null content types fallback to UTF-8
     * Test conditions: Null content type
     * Test result: UTF-8 charset is returned
     * </pre>
     */
    @Test
    void resolveCharsetNullReturnsUtf8() {
        Assertions.assertEquals(StandardCharsets.UTF_8, AccessLogSupport.resolveCharset(null),
                "Null content types should default to UTF-8");
    }

    /**
     * Missing charset defaults to UTF-8.
     *
     * <pre>
     * Theme: Charset resolution
     * Test view: Missing charset defaults to UTF-8
     * Test conditions: text/plain without charset
     * Test result: UTF-8 charset is returned
     * </pre>
     */
    @Test
    void resolveCharsetWithoutCharsetReturnsUtf8() {
        Assertions.assertEquals(StandardCharsets.UTF_8, AccessLogSupport.resolveCharset(CT_TEXT),
                "Missing charset should default to UTF-8");
    }

    /**
     * Valid charset is honored.
     *
     * <pre>
     * Theme: Charset resolution
     * Test view: Valid charset is honored
     * Test conditions: text/plain with ISO-8859-1
     * Test result: ISO-8859-1 charset is returned
     * </pre>
     */
    @Test
    void resolveCharsetValidReturnsExpectedCharset() {
        Assertions.assertEquals(StandardCharsets.ISO_8859_1,
                AccessLogSupport.resolveCharset("text/plain; charset=ISO-8859-1"),
                "Valid charsets should be returned");
    }

    /**
     * Invalid charset falls back to UTF-8.
     *
     * <pre>
     * Theme: Charset resolution
     * Test view: Invalid charset falls back to UTF-8
     * Test conditions: text/plain with bad charset
     * Test result: UTF-8 charset is returned
     * </pre>
     */
    @Test
    void resolveCharsetInvalidReturnsUtf8() {
        Assertions.assertEquals(StandardCharsets.UTF_8,
                AccessLogSupport.resolveCharset("text/plain; charset=bad-charset"),
                "Invalid charset should fall back to UTF-8");
    }
}
