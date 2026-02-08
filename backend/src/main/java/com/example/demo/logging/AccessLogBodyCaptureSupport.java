package com.example.demo.logging;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

/**
 * Shared helpers for request/response body capture policy.
 * 
 * <pre>
 * Responsibilities:
 * 1) Enforce body-capture on/off policy.
 * 2) Decode text payloads using safe charset resolution.
 * 3) Sanitize sensitive body fields and truncate oversized payloads.
 * </pre>
 */
final class AccessLogBodyCaptureSupport {
    /**
     * Utility class.
     * 
     * <pre>
     * Design note:
     * 1) Exposes static helpers only.
     * 2) Constructor is private to prevent instantiation.
     * </pre>
     */
    private AccessLogBodyCaptureSupport() {}

    /**
     * Captures and truncates payload body.
     * 
     * <pre>
     * Algorithm:
     * 1) Return omitted capture when policy disables body logging.
     * 2) Return empty capture when payload is null or empty.
     * 3) Omit non-text payloads for safety.
     * 4) Decode, sanitize, and truncate text payloads.
     * </pre>
     *
     * @param bodyBytes raw body bytes
     * @param contentType content type header
     * @param captureEnabled whether body capture is enabled
     * @return captured body metadata
     */
    /* default */ static AccessLogSupport.BodyCapture captureBody(final byte[] bodyBytes,
            final String contentType, final boolean captureEnabled) {
        AccessLogSupport.BodyCapture capture;
        if (captureEnabled) {
            capture = new AccessLogSupport.BodyCapture(null, false, false);
            if (bodyBytes != null && bodyBytes.length > 0) {
                if (isTextLike(contentType)) {
                    final Charset charset = resolveCharset(contentType);
                    String body = new String(bodyBytes, charset);
                    body = LogSanitizer.sanitizeBody(body, contentType);
                    final boolean truncated = body.length() > AccessLogSupport.MAX_BODY_BYTES;
                    if (truncated) {
                        body = body.substring(0, AccessLogSupport.MAX_BODY_BYTES);
                    }
                    capture = new AccessLogSupport.BodyCapture(body, truncated, false);
                } else {
                    capture = new AccessLogSupport.BodyCapture(null, false, true);
                }
            }
        } else {
            final boolean omitted = bodyBytes != null && bodyBytes.length > 0;
            capture = new AccessLogSupport.BodyCapture(null, false, omitted);
        }
        return capture;
    }

    /**
     * Checks whether the content type is safe to log as text.
     * 
     * <pre>
     * Algorithm:
     * 1) Treat null content type as non-text.
     * 2) Normalize value to lowercase.
     * 3) Accept text, JSON, XML, and form-url-encoded content types.
     * </pre>
     *
     * @param contentType content type header
     * @return true if text-like
     */
    /* default */ static boolean isTextLike(final String contentType) {
        boolean textLike = false;
        if (contentType != null) {
            final String normalized = contentType.toLowerCase(Locale.ROOT);
            textLike = normalized.startsWith("text/") || normalized.contains("application/json")
                    || normalized.contains("+json") || normalized.contains("application/xml")
                    || normalized.contains("+xml")
                    || normalized.contains("application/x-www-form-urlencoded");
        }
        return textLike;
    }

    /**
     * Resolves the charset declared in content type.
     * 
     * <pre>
     * Algorithm:
     * 1) Default to UTF-8.
     * 2) Extract charset token from content type.
     * 3) Resolve charset with fallback to UTF-8 on invalid values.
     * </pre>
     *
     * @param contentType content type header
     * @return resolved charset
     */
    /* default */ static Charset resolveCharset(final String contentType) {
        Charset resolved = StandardCharsets.UTF_8;
        String normalized = null;
        if (contentType != null) {
            normalized = contentType.toLowerCase(Locale.ROOT);
        }
        final int charsetIndex = normalized == null ? -1 : normalized.indexOf("charset=");
        String charset = null;
        if (charsetIndex >= 0) {
            charset = normalized.substring(charsetIndex + "charset=".length()).trim();
        }
        if (charset != null) {
            try {
                resolved = Charset.forName(charset);
            } catch (final IllegalCharsetNameException | UnsupportedCharsetException ex) {
                resolved = StandardCharsets.UTF_8;
            }
        }
        return resolved;
    }
}
