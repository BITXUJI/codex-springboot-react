package com.example.demo.logging;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for access-log body capture. */
class AccessLogFilterBodyCaptureTest {
    /** Text content type used by tests. */
    private static final String CT_TEXT = "text/plain";

    /** Text content type with UTF-8 charset. */
    private static final String CT_TEXT_UTF8 = "text/plain; charset=utf-8";

    /** Binary content type used by tests. */
    private static final String CT_OCTET = "application/octet-stream";

    /** Maximum capture size used by tests. */
    private static final int MAX_CAPTURE = 4096;

    /**
     * Empty payloads are treated as empty text.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Empty payloads are treated as empty text
     * Test conditions: Zero-length body
     * Test result: Omitted flag is false
     * </pre>
     */
    @Test
    void captureBodyEmptyIsNotOmitted() {
        final AccessLogSupport.BodyCapture emptyCapture =
                AccessLogSupport.captureBody(new byte[0], CT_TEXT);

        Assertions.assertEquals(Boolean.FALSE, emptyCapture.omitted(),
                "Empty bodies should not be marked omitted");
    }

    /**
     * Binary payloads are omitted.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Binary payloads are omitted
     * Test conditions: Non-text content type
     * Test result: Omitted flag is true
     * </pre>
     */
    @Test
    void captureBodyBinaryIsOmitted() {
        final byte[] binary = {1, 2};
        final AccessLogSupport.BodyCapture binaryCapture =
                AccessLogSupport.captureBody(binary, CT_OCTET);

        Assertions.assertEquals(Boolean.TRUE, binaryCapture.omitted(),
                "Binary bodies should be marked omitted");
    }

    /**
     * Large payloads are truncated.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Large payloads are truncated
     * Test conditions: Body larger than capture limit
     * Test result: Truncated flag is true
     * </pre>
     */
    @Test
    void captureBodyLongPayloadIsTruncated() {
        final byte[] bigBody = new byte[MAX_CAPTURE + 100];
        final AccessLogSupport.BodyCapture bigCapture =
                AccessLogSupport.captureBody(bigBody, CT_TEXT_UTF8);

        Assertions.assertEquals(Boolean.TRUE, bigCapture.truncated(),
                "Large bodies should be truncated");
    }

    /**
     * Small payloads are not truncated.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Small payloads are not truncated
     * Test conditions: Body smaller than capture limit
     * Test result: Truncated flag is false
     * </pre>
     */
    @Test
    void captureBodySmallPayloadIsNotTruncated() {
        final byte[] smallBody = "ok".getBytes(StandardCharsets.UTF_8);
        final AccessLogSupport.BodyCapture smallCapture =
                AccessLogSupport.captureBody(smallBody, CT_TEXT_UTF8);

        Assertions.assertEquals(Boolean.FALSE, smallCapture.truncated(),
                "Small bodies should not be truncated");
    }

    /**
     * Null payloads are treated as empty.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Null payloads are treated as empty
     * Test conditions: Null body bytes
     * Test result: Omitted flag is false
     * </pre>
     */
    @Test
    void captureBodyNullPayloadIsNotOmitted() {
        final AccessLogSupport.BodyCapture nullCapture =
                AccessLogSupport.captureBody(null, CT_TEXT);

        Assertions.assertEquals(Boolean.FALSE, nullCapture.omitted(),
                "Null bodies should not be marked omitted");
    }

    /**
     * Null content types are omitted for safety.
     *
     * <pre>
     * Theme: Body capture
     * Test view: Null content types are omitted for safety
     * Test conditions: Non-empty body with null content type
     * Test result: Omitted flag is true
     * </pre>
     */
    @Test
    void captureBodyNullContentTypeIsOmitted() {
        final byte[] body = "password=secret".getBytes(StandardCharsets.UTF_8);
        final AccessLogSupport.BodyCapture capture = AccessLogSupport.captureBody(body, null);

        Assertions.assertEquals(Boolean.TRUE, capture.omitted(),
                "Bodies without content type should be omitted");
    }
}
