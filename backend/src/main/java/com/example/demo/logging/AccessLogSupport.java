package com.example.demo.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.StringUtils;

/**
 * Shared helpers used by access logging.
 * 
 * <pre>
 * Responsibilities:
 * 1) Resolve trace id and sanitize request/response headers.
 * 2) Delegate payload capture and client IP resolution to focused helpers.
 * 3) Expose shared constants and body-capture data contracts.
 * </pre>
 */
final class AccessLogSupport {
    /** Request/response header used for trace propagation. */
    /* default */ static final String TRACE_ID_HEADER = "X-Request-Id";

    /** Maximum body size to capture for logs. */
    /* default */ static final int MAX_BODY_BYTES = 4096;

    /**
     * Utility class.
     * 
     * <pre>
     * Design note:
     * 1) Provides stateless static helpers only.
     * 2) Constructor is private to prevent accidental instantiation.
     * </pre>
     */
    private AccessLogSupport() {}

    /**
     * Resolves or generates a trace id.
     * 
     * <pre>
     * Algorithm:
     * 1) Try X-Request-Id from incoming headers.
     * 2) Validate that the value is non-blank.
     * 3) Generate UUID when the header is missing or empty.
     * </pre>
     *
     * @param request current request
     * @return trace identifier
     */
    /* default */ static String resolveTraceId(final HttpServletRequest request) {
        final String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = incoming;
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    /**
     * Resolves client IP address from forwarding headers.
     * 
     * <pre>
     * Algorithm:
     * 1) Start with request.getRemoteAddr() as fallback.
     * 2) Parse X-Forwarded-For only when remote address is in default trusted proxies.
     * 3) Return the resolved client-facing IP value.
     * </pre>
     *
     * @param request current request
     * @return client IP
     */
    /* default */ static String resolveClientIp(final HttpServletRequest request) {
        return AccessLogClientIpResolver.resolveClientIp(request,
                AccessLogClientIpResolver.defaultTrustedProxyAddresses());
    }

    /**
     * Collects and masks request headers.
     * 
     * <pre>
     * Algorithm:
     * 1) Iterate all request header names.
     * 2) Join multiple values into a comma-separated string.
     * 3) Mask sensitive headers before storing them in the output map.
     * </pre>
     *
     * @param request current request
     * @return sanitized headers
     */
    /* default */ static Map<String, String> sanitizeHeaders(final HttpServletRequest request) {
        final Map<String, String> headers = new ConcurrentHashMap<>();
        final Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            final String name = names.nextElement();
            final Enumeration<String> values = request.getHeaders(name);
            final String joined =
                    values == null ? "" : String.join(",", java.util.Collections.list(values));
            headers.put(name, LogSanitizer.maskHeaderValue(name, joined));
        }
        return headers;
    }

    /**
     * Collects and masks response headers.
     * 
     * <pre>
     * Algorithm:
     * 1) Iterate response header names.
     * 2) Replace null header values with an empty string.
     * 3) Mask sensitive values before storing them in the output map.
     * </pre>
     *
     * @param response current response
     * @return sanitized headers
     */
    /* default */ static Map<String, String> sanitizeHeaders(final HttpServletResponse response) {
        final Map<String, String> headers = new ConcurrentHashMap<>();
        for (final String name : response.getHeaderNames()) {
            String value = response.getHeader(name);
            if (value == null) {
                value = "";
            }
            headers.put(name, LogSanitizer.maskHeaderValue(name, value));
        }
        return headers;
    }

    /**
     * Captures and truncates payload body.
     * 
     * <pre>
     * Algorithm:
     * 1) Return an empty capture when payload is null or empty.
     * 2) Omit non-text and unknown content types to avoid sensitive leaks.
     * 3) Decode text payload with resolved charset and sanitize sensitive data.
     * 4) Truncate content above MAX_BODY_BYTES and mark the truncation flag.
     * </pre>
     *
     * @param bodyBytes raw body bytes
     * @param contentType content type header
     * @return captured body metadata
     */
    /* default */ static BodyCapture captureBody(final byte[] bodyBytes, final String contentType) {
        return captureBody(bodyBytes, contentType, true);
    }

    /**
     * Captures and truncates payload body with a policy toggle.
     * 
     * <pre>
     * Algorithm:
     * 1) Return an omitted capture when policy disables body logging.
     * 2) Otherwise delegate to AccessLogBodyCaptureSupport.captureBody(...).
     * </pre>
     *
     * @param bodyBytes raw body bytes
     * @param contentType content type header
     * @param captureEnabled whether payload logging is enabled
     * @return captured body metadata
     */
    /* default */ static BodyCapture captureBody(final byte[] bodyBytes, final String contentType,
            final boolean captureEnabled) {
        return AccessLogBodyCaptureSupport.captureBody(bodyBytes, contentType, captureEnabled);
    }

    /**
     * Checks whether the content type is safe to log as text.
     * 
     * <pre>
     * Algorithm:
     * 1) Delegate content-type classification to AccessLogBodyCaptureSupport.
     * 2) Return true only for text-like content types.
     * </pre>
     *
     * @param contentType content type header
     * @return true if text-like
     */
    /* default */ static boolean isTextLike(final String contentType) {
        return AccessLogBodyCaptureSupport.isTextLike(contentType);
    }

    /**
     * Resolves the charset declared in content type.
     * 
     * <pre>
     * Algorithm:
     * 1) Delegate charset parsing to AccessLogBodyCaptureSupport.
     * 2) Return UTF-8 fallback when parsing fails or charset is missing.
     * </pre>
     *
     * @param contentType content type header
     * @return charset to decode with
     */
    /* default */ static java.nio.charset.Charset resolveCharset(final String contentType) {
        return AccessLogBodyCaptureSupport.resolveCharset(contentType);
    }

    /**
     * Captured body content with metadata.
     * 
     * <pre>
     * Data contract:
     * 1) body contains sanitized text or null.
     * 2) truncated indicates whether body was shortened to MAX_BODY_BYTES.
     * 3) omitted indicates non-text payloads intentionally excluded from logs.
     * </pre>
     *
     * @param body captured body
     * @param truncated whether the body was truncated
     * @param omitted whether the body was omitted
     */
    public record BodyCapture(String body, boolean truncated, boolean omitted) {}
}
