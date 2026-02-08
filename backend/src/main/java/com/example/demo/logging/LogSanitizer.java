package com.example.demo.logging;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
 * 3) Mask sensitive query string parameters.
 * 4) Keep non-sensitive content unchanged for diagnostics.
 * </pre>
 */
public final class LogSanitizer {
    /** JSON field matcher for sensitive keys. */
    private static final Pattern JSON_MASK = Pattern
            .compile("(?i)(\\\"(?:password|passwd|pwd|secret|token|access_token|refresh_token"
                    + "|authorization|auth|apiKey|cardNumber|creditCard|ssn|idCard)"
                    + "\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");

    /** Form URL-encoded matcher for sensitive keys. */
    private static final Pattern FORM_MASK =
            Pattern.compile("(?i)\\b(password|passwd|pwd|secret|token|access_token|refresh_token"
                    + "|authorization|auth|apiKey|cardNumber|creditCard|ssn|idCard)"
                    + "=([^&\\s]+)");

    /** Header names that must be masked. */
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization",
            "proxy-authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token");

    /** Sensitive parameter keys normalized to lowercase alphanumeric form. */
    private static final Set<String> QUERY_SECRET_KEYS =
            Set.of("password", "passwd", "pwd", "secret", "token", "accesstoken", "refreshtoken",
                    "authorization", "auth", "apikey", "cardnumber", "creditcard", "ssn", "idcard");

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

    /**
     * Masks sensitive query parameters.
     * 
     * <pre>
     * Algorithm:
     * 1) Return input unchanged when query is null or blank.
     * 2) Split query by '&amp;' and extract each parameter key.
     * 3) Normalize and compare keys against the sensitive-key set.
     * 4) Replace sensitive parameter values with "****".
     * </pre>
     *
     * @param query raw query string without leading '?'
     * @return sanitized query string
     */
    public static String sanitizeQuery(final String query) {
        String sanitized = query;
        if (query != null && !query.isBlank()) {
            final String[] parts = query.split("&", -1);
            final StringBuilder builder = new StringBuilder(query.length());
            for (int index = 0; index < parts.length; index++) {
                if (index > 0) {
                    builder.append('&');
                }
                builder.append(maskQueryPart(parts[index]));
            }
            sanitized = builder.toString();
        }
        return sanitized;
    }

    /**
     * Checks whether a query parameter key is sensitive.
     * 
     * <pre>
     * Algorithm:
     * 1) Decode percent-encoded key when possible.
     * 2) Normalize key to lowercase alphanumeric form.
     * 3) Compare against the sensitive-key set.
     * </pre>
     *
     * @param key query parameter key
     * @return true when the key should be masked
     */
    private static boolean isSensitiveQueryKey(final String key) {
        String decodedKey;
        try {
            decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ex) {
            decodedKey = key;
        }
        return QUERY_SECRET_KEYS.contains(normalizeKey(decodedKey));
    }

    /**
     * Masks one query key-value segment.
     * 
     * <pre>
     * Algorithm:
     * 1) Split segment into key and optional value.
     * 2) Check whether the key is sensitive.
     * 3) Mask value when the segment is key=value.
     * </pre>
     *
     * @param part one query segment
     * @return sanitized segment
     */
    private static String maskQueryPart(final String part) {
        final int equalsIndex = part.indexOf('=');
        final String key = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
        String maskedPart = part;
        if (isSensitiveQueryKey(key) && equalsIndex >= 0) {
            maskedPart = key + "=****";
        }
        return maskedPart;
    }

    /**
     * Normalizes parameter keys for case-insensitive matching.
     * 
     * <pre>
     * Algorithm:
     * 1) Convert key to lowercase using Locale.ROOT.
     * 2) Keep only ASCII letters and digits.
     * 3) Return the normalized string.
     * </pre>
     *
     * @param key raw key
     * @return normalized key
     */
    private static String normalizeKey(final String key) {
        final String lower = key.toLowerCase(Locale.ROOT);
        final StringBuilder normalized = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            final char current = lower.charAt(index);
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }
}
