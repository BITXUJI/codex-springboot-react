package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for client IP resolution. */
class AccessLogFilterClientIpTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Forwarded header name used by tests. */
    private static final String FORWARDED_HEADER = "X-Forwarded-For";

    /** HTTP GET method name. */
    private static final String METHOD_GET = "GET";

    /** IP format string used by tests. */
    private static final String IP_FORMAT = "%d.%d.%d.%d";

    /**
     * Forwarded list uses first IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Forwarded list uses first IP
     * Test conditions: X-Forwarded-For contains multiple values
     * Test result: First IP is returned
     * </pre>
     */
    @Test
    void resolveClientIpForwardedCommaReturnsFirst() {
        final MockHttpServletRequest forwarded = new MockHttpServletRequest(METHOD_GET, PATH_API);
        forwarded.setRemoteAddr(String.format(IP_FORMAT, 10, 0, 0, 1));
        forwarded.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 1) + ", "
                + String.format(IP_FORMAT, 70, 0, 0, 1));

        Assertions.assertEquals(String.format(IP_FORMAT, 203, 0, 113, 1),
                AccessLogSupport.resolveClientIp(forwarded), "First forwarded IP should be used");
    }

    /**
     * Forwarded single IP is used.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Forwarded single IP is used
     * Test conditions: X-Forwarded-For contains one value
     * Test result: That IP is returned
     * </pre>
     */
    @Test
    void resolveClientIpForwardedSingleReturnsValue() {
        final MockHttpServletRequest forwarded = new MockHttpServletRequest(METHOD_GET, PATH_API);
        forwarded.setRemoteAddr(String.format(IP_FORMAT, 10, 0, 0, 3));
        forwarded.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 2));

        Assertions.assertEquals(String.format(IP_FORMAT, 203, 0, 113, 2),
                AccessLogSupport.resolveClientIp(forwarded),
                "Forwarded IP should be used when present");
    }

    /**
     * Remote address used when no forwarded header.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Remote address used when no forwarded header
     * Test conditions: Missing X-Forwarded-For
     * Test result: Remote address is returned
     * </pre>
     */
    @Test
    void resolveClientIpUsesRemoteAddrWhenMissingForwarded() {
        final MockHttpServletRequest direct = new MockHttpServletRequest(METHOD_GET, PATH_API);
        direct.setRemoteAddr(String.format(IP_FORMAT, 10, 0, 0, 2));

        Assertions.assertEquals(String.format(IP_FORMAT, 10, 0, 0, 2),
                AccessLogSupport.resolveClientIp(direct),
                "Remote address should be used when no forwarded header exists");
    }

    /**
     * Untrusted remote ignores forwarded IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Untrusted remote ignores forwarded IP
     * Test conditions: Public remote address sends X-Forwarded-For
     * Test result: Remote address is returned
     * </pre>
     */
    @Test
    void resolveClientIpIgnoresForwardedForUntrustedRemote() {
        final MockHttpServletRequest direct = new MockHttpServletRequest(METHOD_GET, PATH_API);
        direct.setRemoteAddr(String.format(IP_FORMAT, 198, 51, 100, 10));
        direct.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 9));

        Assertions.assertEquals(String.format(IP_FORMAT, 198, 51, 100, 10),
                AccessLogSupport.resolveClientIp(direct),
                "Forwarded header should be ignored for untrusted remote addresses");
    }

    /**
     * Invalid remote address ignores forwarded IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Invalid remote address ignores forwarded IP
     * Test conditions: Remote address is unresolvable and X-Forwarded-For exists
     * Test result: Original remote value is returned
     * </pre>
     */
    @Test
    void resolveClientIpIgnoresForwardedWhenRemoteInvalid() {
        final MockHttpServletRequest direct = new MockHttpServletRequest(METHOD_GET, PATH_API);
        direct.setRemoteAddr("invalid host");
        direct.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 10));

        Assertions.assertEquals("invalid host", AccessLogSupport.resolveClientIp(direct),
                "Forwarded header should be ignored when remote address is invalid");
    }

    /**
     * Blank remote address ignores forwarded IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Blank remote address ignores forwarded IP
     * Test conditions: Remote address is blank and X-Forwarded-For exists
     * Test result: Blank remote value is returned
     * </pre>
     */
    @Test
    void resolveClientIpIgnoresForwardedWhenRemoteBlank() {
        final MockHttpServletRequest direct = new MockHttpServletRequest(METHOD_GET, PATH_API);
        direct.setRemoteAddr("");
        direct.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 11));

        Assertions.assertEquals("", AccessLogSupport.resolveClientIp(direct),
                "Forwarded header should be ignored when remote address is blank");
    }

    /**
     * Loopback remote accepts forwarded IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Loopback remote accepts forwarded IP
     * Test conditions: Remote address is loopback and X-Forwarded-For exists
     * Test result: Forwarded client IP is returned
     * </pre>
     */
    @Test
    void resolveClientIpUsesForwardedForLoopbackProxy() {
        final MockHttpServletRequest forwarded = new MockHttpServletRequest(METHOD_GET, PATH_API);
        forwarded.setRemoteAddr(String.format(IP_FORMAT, 127, 0, 0, 1));
        forwarded.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 12));

        Assertions.assertEquals(String.format(IP_FORMAT, 203, 0, 113, 12),
                AccessLogSupport.resolveClientIp(forwarded),
                "Forwarded IP should be used for loopback proxy addresses");
    }

    /**
     * Link-local remote accepts forwarded IP.
     *
     * <pre>
     * Theme: Client IP resolution
     * Test view: Link-local remote accepts forwarded IP
     * Test conditions: Remote address is link-local and X-Forwarded-For exists
     * Test result: Forwarded client IP is returned
     * </pre>
     */
    @Test
    void resolveClientIpUsesForwardedForLinkLocalProxy() {
        final MockHttpServletRequest forwarded = new MockHttpServletRequest(METHOD_GET, PATH_API);
        forwarded.setRemoteAddr(String.format(IP_FORMAT, 169, 254, 1, 10));
        forwarded.addHeader(FORWARDED_HEADER, String.format(IP_FORMAT, 203, 0, 113, 13));

        Assertions.assertEquals(String.format(IP_FORMAT, 203, 0, 113, 13),
                AccessLogSupport.resolveClientIp(forwarded),
                "Forwarded IP should be used for link-local proxy addresses");
    }
}
