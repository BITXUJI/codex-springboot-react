package com.example.demo.logging;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Sanitizes log content to avoid leaking sensitive data. */
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

    /** Utility class. */
    private LogSanitizer() {}

    /**
     * Masks sensitive fields in request/response bodies.
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
