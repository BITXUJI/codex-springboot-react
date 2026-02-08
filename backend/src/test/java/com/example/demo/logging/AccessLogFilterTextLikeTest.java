package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for text-like content detection. */
class AccessLogFilterTextLikeTest {
    /** Text content type used by tests. */
    private static final String CT_TEXT = "text/plain";

    /** JSON content type used by tests. */
    private static final String CT_JSON = "application/json";

    /** XML content type used by tests. */
    private static final String CT_XML = "application/xml";

    /** JSON vendor content type used by tests. */
    private static final String CT_JSON_VENDOR = "application/vnd.test+json";

    /** XML vendor content type used by tests. */
    private static final String CT_XML_VENDOR = "application/vnd.test+xml";

    /** Form content type used by tests. */
    private static final String CT_FORM = "application/x-www-form-urlencoded";

    /** Binary content type used by tests. */
    private static final String CT_OCTET = "application/octet-stream";

    /**
     * Null content types are not treated as text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: Null content types are not treated as text-like
     * Test conditions: Null content type
     * Test result: Text-like returns false
     * </pre>
     */
    @Test
    void isTextLikeNullReturnsFalse() {
        Assertions.assertEquals(Boolean.FALSE, AccessLogSupport.isTextLike(null),
                "Null content types should not be considered text-like");
    }

    /**
     * text/* content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: text/* content is text-like
     * Test conditions: text/plain content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeTextReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_TEXT),
                "text/plain should be considered text-like");
    }

    /**
     * JSON content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: JSON content is text-like
     * Test conditions: application/json content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeJsonReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_JSON),
                "application/json should be considered text-like");
    }

    /**
     * Vendor JSON content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: Vendor JSON content is text-like
     * Test conditions: application/vnd.*+json content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeJsonVendorReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_JSON_VENDOR),
                "Vendor JSON content should be considered text-like");
    }

    /**
     * XML content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: XML content is text-like
     * Test conditions: application/xml content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeXmlReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_XML),
                "application/xml should be considered text-like");
    }

    /**
     * Vendor XML content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: Vendor XML content is text-like
     * Test conditions: application/vnd.*+xml content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeXmlVendorReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_XML_VENDOR),
                "Vendor XML content should be considered text-like");
    }

    /**
     * Form content is text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: Form content is text-like
     * Test conditions: application/x-www-form-urlencoded content type
     * Test result: Text-like returns true
     * </pre>
     */
    @Test
    void isTextLikeFormReturnsTrue() {
        Assertions.assertEquals(Boolean.TRUE, AccessLogSupport.isTextLike(CT_FORM),
                "Form content should be considered text-like");
    }

    /**
     * Binary content is not text-like.
     *
     * <pre>
     * Theme: Content type detection
     * Test view: Binary content is not text-like
     * Test conditions: application/octet-stream content type
     * Test result: Text-like returns false
     * </pre>
     */
    @Test
    void isTextLikeBinaryReturnsFalse() {
        Assertions.assertEquals(Boolean.FALSE, AccessLogSupport.isTextLike(CT_OCTET),
                "Binary content should not be considered text-like");
    }
}
