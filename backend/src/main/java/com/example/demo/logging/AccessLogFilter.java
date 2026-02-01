package com.example.demo.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Logs structured access logs with request/response payloads and trace IDs. */
@Component
@NoArgsConstructor
public class AccessLogFilter extends OncePerRequestFilter {
    /** Logger dedicated to access logs. */
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    /** Request/response header used for trace propagation. */
    private static final String TRACE_ID_HEADER = "X-Request-Id";

    /** Maximum body size to capture for logs. */
    private static final int MAX_BODY_BYTES = 4096;

    /** Paths that should not emit access logs. */
    private static final String[] SKIP_PATHS = {"/actuator", "/health"};

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        boolean skip = false;
        final String path = request.getRequestURI();
        if (StringUtils.hasText(path)) {
            for (final String prefix : SKIP_PATHS) {
                if (path.startsWith(prefix)) {
                    skip = true;
                    break;
                }
            }
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, MAX_BODY_BYTES);
        final ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        final String traceId = resolveTraceId(requestWrapper);
        MDC.put("traceId", traceId);
        responseWrapper.setHeader(TRACE_ID_HEADER, traceId);

        final long startNanos = System.nanoTime();
        Exception failure = null;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (final IOException | ServletException ex) {
            failure = ex;
            throw ex;
        } finally {
            final long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logAccess(requestWrapper, responseWrapper, durationMs, failure);
            MDC.clear();
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Emits structured access log payload.
     *
     * @param request cached request
     * @param response cached response
     * @param durationMs request duration
     * @param failure thrown exception, if any
     */
    protected void logAccess(final ContentCachingRequestWrapper request,
            final ContentCachingResponseWrapper response, final long durationMs,
            final Exception failure) {
        final String reqContentType = request.getContentType();
        final String respContentType = response.getContentType();

        final BodyCapture requestBody =
                captureBody(request.getContentAsByteArray(), reqContentType);
        final BodyCapture responseBody =
                captureBody(response.getContentAsByteArray(), respContentType);

        int status = response.getStatus();
        if (failure != null && status < 400) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        final Map<String, String> requestHeaders = sanitizeHeaders(request);
        final Map<String, String> responseHeaders = sanitizeHeaders(response);

        if (ACCESS_LOG.isInfoEnabled()) {
            ACCESS_LOG.info("access", keyValue("traceId", MDC.get("traceId")),
                    keyValue("method", request.getMethod()),
                    keyValue("path", request.getRequestURI()),
                    keyValue("query", request.getQueryString()), keyValue("status", status),
                    keyValue("durationMs", durationMs),
                    keyValue("clientIp", resolveClientIp(request)),
                    keyValue("remoteAddr", request.getRemoteAddr()),
                    keyValue("userAgent", request.getHeader("User-Agent")),
                    keyValue("requestHeaders", requestHeaders),
                    keyValue("responseHeaders", responseHeaders),
                    keyValue("requestBody", requestBody.body()),
                    keyValue("responseBody", responseBody.body()),
                    keyValue("requestBodyTruncated", requestBody.truncated()),
                    keyValue("responseBodyTruncated", responseBody.truncated()),
                    keyValue("requestBodyOmitted", requestBody.omitted()),
                    keyValue("responseBodyOmitted", responseBody.omitted()),
                    keyValue("exception", failure == null ? null : failure.getClass().getName()));
        }
    }

    /**
     * Resolves or generates a trace id.
     *
     * @param request current request
     * @return trace identifier
     */
    protected static String resolveTraceId(final HttpServletRequest request) {
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
     * @param request current request
     * @return client IP
     */
    protected static String resolveClientIp(final HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            final int commaIndex = forwarded.indexOf(',');
            clientIp =
                    commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return clientIp;
    }

    /**
     * Collects and masks request headers.
     *
     * @param request current request
     * @return sanitized headers
     */
    protected static Map<String, String> sanitizeHeaders(final HttpServletRequest request) {
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
     * @param response current response
     * @return sanitized headers
     */
    protected static Map<String, String> sanitizeHeaders(final HttpServletResponse response) {
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
     * @param bodyBytes raw body bytes
     * @param contentType content type header
     * @return captured body metadata
     */
    protected static BodyCapture captureBody(final byte[] bodyBytes, final String contentType) {
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
     * @param contentType content type header
     * @return true if text-like
     */
    protected static boolean isTextLike(final String contentType) {
        boolean textLike = true;
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
     * @param contentType content type header
     * @return charset to decode with
     */
    protected static Charset resolveCharset(final String contentType) {
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
     * @param body captured body
     * @param truncated whether the body was truncated
     * @param omitted whether the body was omitted
     */
    protected record BodyCapture(String body, boolean truncated, boolean omitted) {}
}
