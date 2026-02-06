package com.example.demo.logging;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanitizes log content to avoid leaking sensitive data.
 * 
 * <pre>
 * Responsibilities:
 * 1) Mask sensitive key-value pairs in JSON and form payloads.
 * 2) Mask known secret-bearing HTTP headers.
 * 3) Keep non-sensitive content unchanged for diagnostics.
 * </pre>
 */
public final class LogSanitizer {
    /** JSON field matcher for sensitive keys. */
    private static final Pattern JSON_MASK = Pattern
            .compile("(?i)(\\\"(?:password|passwd|pwd|secret|token|access_token|refresh_token"
                    + "|authorization|auth|apiKey|apikey|cardNumber|creditCard|ssn|idCard)"
                    + "\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");

    /** Form URL-encoded matcher for sensitive keys. */
    private static final Pattern FORM_MASK =
            Pattern.compile("(?i)\\b(password|passwd|pwd|secret|token|access_token|refresh_token"
                    + "|authorization|auth|apiKey|apikey|cardNumber|creditCard|ssn|idCard)"
                    + "=([^&\\s]+)");

    /** Header names that must be masked. */
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization",
            "proxy-authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token");

    /**
     * Utility class.
     * 
     * <pre>
     * Design note:
     * 1) Exposes only static sanitization helpers.
     * 2) Private constructor blocks accidental instantiation.
     * </pre>
     */
    private LogSanitizer() {}

    /**
     * Masks sensitive fields in request/response bodies.
     * 
     * <pre>
     * Algorithm:
     * 1) Return input unchanged when body is null or blank.
     * 2) Normalize content type to select applicable masking rules.
     * 3) Apply JSON and/or form regex masking for sensitive keys.
     * </pre>
     *
     * @param body raw body
     * @param contentType content type header
     * @return sanitized body
     */
    public static String sanitizeBody(final String body, final String contentType) {
        String sanitized = body;
        if (body != null && !body.isBlank()) {
            final String normalized =
                    contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
            if (normalized.contains("application/json") || normalized.contains("+json")) {
                sanitized = JSON_MASK.matcher(sanitized).replaceAll("$1****$3");
            }
            if (normalized.contains("application/x-www-form-urlencoded")) {
                sanitized = FORM_MASK.matcher(sanitized).replaceAll("$1=****");
            }
        }
        return sanitized;
    }

    /**
     * Masks sensitive headers.
     * 
     * <pre>
     * Algorithm:
     * 1) Return null when header value is null.
     * 2) Normalize header name to lowercase.
     * 3) Replace sensitive header values with "****".
     * </pre>
     *
     * @param headerName header name
     * @param headerValue header value
     * @return masked value
     */
    public static String maskHeaderValue(final String headerName, final String headerValue) {
        String masked = headerValue;
        if (headerValue != null) {
            final String normalized = headerName == null ? "" : headerName.toLowerCase(Locale.ROOT);
            if (SENSITIVE_HEADERS.contains(normalized)) {
                masked = "****";
            }
        }
        return masked;
    }
}
