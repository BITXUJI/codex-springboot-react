package com.example.demo.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.StringUtils;

/**
 * Shared helpers used by access logging.
 * 
 * <pre>
 * Responsibilities:
 * 1) Resolve trace id and client IP metadata for access logs.
 * 2) Sanitize headers and payload bodies before they are logged.
 * 3) Apply consistent text/binary detection and charset handling.
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
     * 2) Parse X-Forwarded-For only when remote address is a local proxy range.
     * 3) Return the resolved client-facing IP value.
     * </pre>
     *
     * @param request current request
     * @return client IP
     */
    /* default */ static String resolveClientIp(final HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        final String forwarded = request.getHeader("X-Forwarded-For");
        final String remoteAddr = request.getRemoteAddr();
        boolean trustedProxy = false;
        if (StringUtils.hasText(remoteAddr)) {
            try {
                final InetAddress address = InetAddress.getByName(remoteAddr);
                trustedProxy = address.isLoopbackAddress() || address.isSiteLocalAddress()
                        || address.isLinkLocalAddress();
            } catch (final UnknownHostException ex) {
                trustedProxy = false;
            }
        }
        if (StringUtils.hasText(forwarded) && trustedProxy) {
            final int commaIndex = forwarded.indexOf(',');
            clientIp =
                    commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return clientIp;
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
        BodyCapture capture = new BodyCapture(null, false, false);
        if (bodyBytes != null && bodyBytes.length > 0) {
            if (isTextLike(contentType)) {
                final Charset charset = resolveCharset(contentType);
                String body = new String(bodyBytes, charset);
                body = LogSanitizer.sanitizeBody(body, contentType);
                final boolean truncated = body.length() > MAX_BODY_BYTES;
                if (truncated) {
                    body = body.substring(0, MAX_BODY_BYTES);
                }
                capture = new BodyCapture(body, truncated, false);
            } else {
                capture = new BodyCapture(null, false, true);
            }
        }
        return capture;
    }

    /**
     * Checks whether the content type is safe to log as text.
     * 
     * <pre>
     * Algorithm:
     * 1) Treat null content type as non-text to avoid accidental secret leaks.
     * 2) Normalize value to lowercase.
     * 3) Accept text/*, JSON, XML, and form-url-encoded content types.
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
     * 2) Extract charset=... token when provided.
     * 3) Resolve with Charset.forName and fallback to UTF-8 on invalid values.
     * </pre>
     *
     * @param contentType content type header
     * @return charset to decode with
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
