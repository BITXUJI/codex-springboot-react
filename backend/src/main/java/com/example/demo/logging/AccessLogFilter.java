package com.example.demo.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Logs structured access logs with request/response payloads and trace IDs.
 * 
 * <pre>
 * Responsibilities:
 * 1) Intercept each HTTP request once and measure request duration.
 * 2) Capture sanitized request/response metadata for observability.
 * 3) Propagate a trace identifier across logging context and response headers.
 * </pre>
 */
@Component
@NoArgsConstructor
public class AccessLogFilter extends OncePerRequestFilter {
    /** Logger dedicated to access logs. */
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    /** MDC key used to store and read trace id values. */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    /** Request/response header used for trace propagation. */
    private static final String TRACE_ID_HEADER = "X-Request-Id";

    /** Maximum body size to capture for logs. */
    private static final int MAX_BODY_BYTES = 4096;

    /** Paths that should not emit access logs. */
    private static final String[] SKIP_PATHS = {"/actuator", "/health"};

    /**
     * Determines whether the current request should skip logging.
     * 
     * <pre>
     * Algorithm:
     * 1) Read request URI from the current request.
     * 2) Compare the path against known skip prefixes.
     * 3) Return true when any prefix matches, otherwise false.
     * </pre>
     *
     * @param request current request
     * @return true to skip logging
     */
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

    /**
     * Main filter method to log access.
     * 
     * <pre>
     * Algorithm:
     * 1) Wrap request/response with content-caching wrappers.
     * 2) Resolve trace ID, bind it to MDC, and write it to response headers.
     * 3) Execute downstream filter chain and capture raised checked exceptions.
     * 4) Log access details and restore previous MDC context.
     * 5) Copy cached response body back to the client.
     * </pre>
     *
     * @param request current request
     * @param response current response
     * @param filterChain filter chain
     * @throws ServletException servlet exception
     * @throws IOException IO exception
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, MAX_BODY_BYTES);
        final ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        final Map<String, String> priorMdc = MDC.getCopyOfContextMap();
        final String traceId = resolveTraceId(requestWrapper);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        responseWrapper.setHeader(TRACE_ID_HEADER, traceId);

        final long startNanos = System.nanoTime();
        Exception failure = null;
        boolean completed = false;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            completed = true;
        } catch (final IOException | ServletException ex) {
            failure = ex;
            throw ex;
        } finally {
            if (!completed && failure == null) {
                failure = new ServletException("Request failed before completion");
            }
            final long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            logAccess(requestWrapper, responseWrapper, durationMs, failure);
            if (priorMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(priorMdc);
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Emits structured access log payload.
     * 
     * <pre>
     * Algorithm:
     * 1) Capture sanitized request/response bodies and headers.
     * 2) Derive final status code, including failure fallback to 500.
     * 3) Write one structured "access" log event with all key attributes.
     * </pre>
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
            final LoggingEventBuilder logBuilder = ACCESS_LOG.atInfo()
                    .addArgument(keyValue(TRACE_ID_MDC_KEY, MDC.get(TRACE_ID_MDC_KEY)))
                    .addArgument(keyValue("method", request.getMethod()))
                    .addArgument(keyValue("path", request.getRequestURI()))
                    .addArgument(keyValue("query", request.getQueryString()))
                    .addArgument(keyValue("status", status))
                    .addArgument(keyValue("durationMs", durationMs))
                    .addArgument(keyValue("clientIp", resolveClientIp(request)))
                    .addArgument(keyValue("remoteAddr", request.getRemoteAddr()))
                    .addArgument(keyValue("userAgent", request.getHeader("User-Agent")))
                    .addArgument(keyValue("requestHeaders", requestHeaders))
                    .addArgument(keyValue("responseHeaders", responseHeaders))
                    .addArgument(keyValue("requestBody", requestBody.body()))
                    .addArgument(keyValue("responseBody", responseBody.body()))
                    .addArgument(keyValue("requestBodyTruncated", requestBody.truncated()))
                    .addArgument(keyValue("responseBodyTruncated", responseBody.truncated()))
                    .addArgument(keyValue("requestBodyOmitted", requestBody.omitted()))
                    .addArgument(keyValue("responseBodyOmitted", responseBody.omitted()))
                    .addArgument(keyValue("exception",
                            failure == null ? null : failure.getClass().getName()));
            logBuilder.log("access");
        }
    }

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
    protected static String resolveClientIp(final HttpServletRequest request) {
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
     * <pre>
     * Algorithm:
     * 1) Return an empty capture when payload is null or empty.
     * 2) Omit non-text content types to avoid binary log noise.
     * 3) Decode text payload with resolved charset and sanitize sensitive data.
     * 4) Truncate content above MAX_BODY_BYTES and mark the truncation flag.
     * </pre>
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
     * <pre>
     * Algorithm:
     * 1) Treat null content type as text-like for backward compatibility.
     * 2) Normalize value to lowercase.
     * 3) Accept text/*, JSON, XML, and form-url-encoded content types.
     * </pre>
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
    protected record BodyCapture(String body, boolean truncated, boolean omitted) {}
}
