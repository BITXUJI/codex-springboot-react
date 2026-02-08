package com.example.demo.logging;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
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
 * 2) Delegate metadata sanitization to dedicated helpers.
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

    /** Paths that should not emit access logs. */
    private static final String[] SKIP_PATHS = {"/actuator", "/health"};

    /** Default trusted proxy list when configuration is absent. */
    private static final String TRUSTED_PROXIES = "127.0.0.1,::1,0:0:0:0:0:0:0:1";

    /** Trusted proxy addresses allowed to forward client IP headers. */
    private Set<String> trustedProxies = AccessLogClientIpResolver.defaultTrustedProxyAddresses();

    /** Whether request payload logging is enabled. */
    private boolean reqBodyCapture;

    /** Whether response payload logging is enabled. */
    private boolean resBodyCapture;

    /**
     * Configures trusted proxy addresses.
     * 
     * <pre>
     * Algorithm:
     * 1) Read comma-separated proxy addresses from configuration.
     * 2) Parse and normalize entries.
     * 3) Update trustedProxies with parsed values.
     * </pre>
     *
     * @param trustedProxies comma-separated proxy addresses
     */
    @Value("${app.logging.access.trusted-proxies:" + TRUSTED_PROXIES + "}")
    /* default */ void setTrustedProxies(final String trustedProxies) {
        this.trustedProxies = AccessLogClientIpResolver.parseTrustedProxyAddresses(trustedProxies);
    }

    /**
     * Configures request body capture.
     * 
     * <pre>
     * Algorithm:
     * 1) Read boolean flag from configuration.
     * 2) Update reqBodyCapture toggle.
     * </pre>
     *
     * @param enabled whether request body capture is enabled
     */
    @Value("${app.logging.access.capture-request-body:false}")
    /* default */ void setCaptureRequestBody(final boolean enabled) {
        reqBodyCapture = enabled;
    }

    /**
     * Configures response body capture.
     * 
     * <pre>
     * Algorithm:
     * 1) Read boolean flag from configuration.
     * 2) Update resBodyCapture toggle.
     * </pre>
     *
     * @param enabled whether response body capture is enabled
     */
    @Value("${app.logging.access.capture-response-body:false}")
    /* default */ void setCaptureResponseBody(final boolean enabled) {
        resBodyCapture = enabled;
    }

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
     * 3) Execute downstream filter chain and preserve thrown failure type.
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
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, AccessLogSupport.MAX_BODY_BYTES);
        final ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        final Map<String, String> priorMdc = MDC.getCopyOfContextMap();
        final String traceId = AccessLogSupport.resolveTraceId(requestWrapper);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        responseWrapper.setHeader(AccessLogSupport.TRACE_ID_HEADER, traceId);

        final long startNanos = System.nanoTime();
        Exception failure = null;
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (final IOException | ServletException ex) {
            failure = ex;
            throw ex;
        } catch (final RuntimeException ex) {
            failure = ex;
            throw new ServletException(ex);
        } finally {
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
     * 1) Capture sanitized request/response bodies and headers via helper utilities.
     * 2) Derive final status code, including failure fallback to 500.
     * 3) Write one structured "access" log event with key attributes.
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
        final AccessLogSupport.BodyCapture requestBody = AccessLogBodyCaptureSupport.captureBody(
                request.getContentAsByteArray(), request.getContentType(), reqBodyCapture);
        final AccessLogSupport.BodyCapture responseBody = AccessLogBodyCaptureSupport.captureBody(
                response.getContentAsByteArray(), response.getContentType(), resBodyCapture);

        int status = response.getStatus();
        if (failure != null && status < 400) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        final Map<String, String> requestHeaders = AccessLogSupport.sanitizeHeaders(request);
        final Map<String, String> responseHeaders = AccessLogSupport.sanitizeHeaders(response);

        if (ACCESS_LOG.isInfoEnabled()) {
            final LoggingEventBuilder logBuilder = ACCESS_LOG.atInfo()
                    .addArgument(keyValue("method", request.getMethod()))
                    .addArgument(keyValue("path", request.getRequestURI()))
                    .addArgument(
                            keyValue("query", LogSanitizer.sanitizeQuery(request.getQueryString())))
                    .addArgument(keyValue("status", status))
                    .addArgument(keyValue("durationMs", durationMs))
                    .addArgument(keyValue("clientIp",
                            AccessLogClientIpResolver.resolveClientIp(request, trustedProxies)))
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
}
