package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link LogSanitizer} query masking behavior. */
class LogSanitizerQueryTest {
    /**
     * Sensitive query parameters are masked.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Sensitive query parameters are masked
     * Test conditions: Query contains token and normal parameters
     * Test result: Sensitive values are replaced while non-sensitive values remain
     * </pre>
     */
    @Test
    void sanitizeQueryMasksSensitiveValues() {
        final String query = "token=abc&name=demo&apiKey=xyz";

        Assertions.assertEquals("token=****&name=demo&apiKey=****",
                LogSanitizer.sanitizeQuery(query), "Sensitive query parameters should be masked");
    }

    /**
     * Null query values are preserved.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Null query values are preserved
     * Test conditions: Query is null
     * Test result: Null is returned
     * </pre>
     */
    @Test
    void sanitizeQueryKeepsNull() {
        Assertions.assertNull(LogSanitizer.sanitizeQuery(null), "Null queries should remain null");
    }

    /**
     * Blank query values are preserved.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Blank query values are preserved
     * Test conditions: Query is blank
     * Test result: Blank value is returned unchanged
     * </pre>
     */
    @Test
    void sanitizeQueryKeepsBlank() {
        Assertions.assertEquals("  ", LogSanitizer.sanitizeQuery("  "),
                "Blank queries should remain unchanged");
    }

    /**
     * Sensitive keys without explicit values are preserved as keys only.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Sensitive keys without explicit values are preserved as keys only
     * Test conditions: Query contains key-only token and plain key
     * Test result: Sensitive key remains key-only.
     * Non-sensitive key remains unchanged.
     * </pre>
     */
    @Test
    void sanitizeQueryHandlesKeyOnlyParameters() {
        Assertions.assertEquals("token&name", LogSanitizer.sanitizeQuery("token&name"),
                "Key-only parameters should preserve key structure");
    }

    /**
     * Encoded sensitive keys are detected and masked.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Encoded sensitive keys are detected and masked
     * Test conditions: Query contains encoded access_token key
     * Test result: Sensitive value is masked
     * </pre>
     */
    @Test
    void sanitizeQueryMasksEncodedSensitiveKey() {
        Assertions.assertEquals("access%5Ftoken=****",
                LogSanitizer.sanitizeQuery("access%5Ftoken=abc"),
                "Encoded sensitive keys should be masked");
    }

    /**
     * Malformed encoding falls back to raw key matching.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Malformed encoding falls back to raw key matching
     * Test conditions: Query contains malformed encoded key
     * Test result: Query remains stable without exceptions
     * </pre>
     */
    @Test
    void sanitizeQueryHandlesMalformedEncoding() {
        Assertions.assertEquals("%E0%A4%A=abc", LogSanitizer.sanitizeQuery("%E0%A4%A=abc"),
                "Malformed encoding should not break sanitization");
    }

    /**
     * Separator variants still match sensitive keys.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Separator variants still match sensitive keys
     * Test conditions: Query uses api-key format
     * Test result: Sensitive value is masked
     * </pre>
     */
    @Test
    void sanitizeQueryMasksKeyWithSeparator() {
        Assertions.assertEquals("api-key=****", LogSanitizer.sanitizeQuery("api-key=abc"),
                "Normalized key matching should handle separators");
    }

    /**
     * Numeric key characters are preserved during normalization.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Numeric key characters are preserved during normalization
     * Test conditions: Query key includes digits
     * Test result: Non-sensitive value remains unchanged
     * </pre>
     */
    @Test
    void sanitizeQueryKeepsNumericKeys() {
        Assertions.assertEquals("field1=value", LogSanitizer.sanitizeQuery("field1=value"),
                "Numeric key characters should not break sanitization");
    }

    /**
     * Symbol-only keys remain stable after normalization checks.
     *
     * <pre>
     * Theme: Query masking
     * Test view: Symbol-only keys remain stable after normalization checks
     * Test conditions: Query key contains non-alphanumeric symbol above 'z'
     * Test result: Value remains unchanged for non-sensitive keys
     * </pre>
     */
    @Test
    void sanitizeQueryKeepsSymbolKeys() {
        Assertions.assertEquals("~=value", LogSanitizer.sanitizeQuery("~=value"),
                "Symbol keys should remain unchanged");
    }
}
