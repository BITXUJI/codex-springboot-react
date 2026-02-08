package com.example.demo.logging;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for trusted-proxy based client IP resolution. */
class AccessLogClientIpResolverTest {
    /** API path used by tests. */
    private static final String PATH_API = "/api/hello";

    /** Forwarded header key used by tests. */
    private static final String FORWARDED_HEADER = "X-Forwarded-For";

    /** HTTP method used by tests. */
    private static final String METHOD_GET = "GET";

    /** IPv4 format string used by tests. */
    private static final String IP_FORMAT = "%d.%d.%d.%d";

    /**
     * Forwarded headers are ignored for non-allowlisted remotes.
     *
     * <pre>
     * Theme: Trusted proxy resolution
     * Test view: Forwarded headers are ignored for non-allowlisted remotes
     * Test conditions: Remote address is not in trusted proxy set
     * Test result: Remote address is returned
     * </pre>
     */
    @Test
    void resolveClientIpIgnoresForwardedWhenRemoteNotTrusted() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        request.setRemoteAddr(ipv4Address(10, 0, 0, 5));
        request.addHeader(FORWARDED_HEADER, ipv4Address(203, 0, 113, 20));

        final String clientIp = AccessLogClientIpResolver.resolveClientIp(request,
                Set.of(ipv4Address(127, 0, 0, 1)));
        Assertions.assertEquals(ipv4Address(10, 0, 0, 5), clientIp,
                "Forwarded value must be ignored for untrusted proxies");
    }

    /**
     * Forwarded headers are honored for allowlisted remotes.
     *
     * <pre>
     * Theme: Trusted proxy resolution
     * Test view: Forwarded headers are honored for allowlisted remotes
     * Test conditions: Remote address is in trusted proxy set
     * Test result: First forwarded IP is returned
     * </pre>
     */
    @Test
    void resolveClientIpUsesForwardedWhenRemoteTrusted() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_API);
        request.setRemoteAddr(ipv4Address(10, 0, 0, 5));
        request.addHeader(FORWARDED_HEADER,
                ipv4Address(203, 0, 113, 20) + ", " + ipv4Address(70, 0, 0, 1));

        final String clientIp = AccessLogClientIpResolver.resolveClientIp(request,
                Set.of(ipv4Address(10, 0, 0, 5)));
        Assertions.assertEquals(ipv4Address(203, 0, 113, 20), clientIp,
                "First forwarded hop should be used for trusted proxies");
    }

    /**
     * Null trusted-proxy configuration yields an empty set.
     *
     * <pre>
     * Theme: Trusted proxy resolution
     * Test view: Null trusted-proxy configuration yields an empty set
     * Test conditions: Parser receives null input
     * Test result: Parsed set is empty
     * </pre>
     */
    @Test
    void parseTrustedProxyAddressesHandlesNullInput() {
        Assertions.assertTrue(AccessLogClientIpResolver.parseTrustedProxyAddresses(null).isEmpty(),
                "Null configuration should produce an empty trusted proxy set");
    }

    /**
     * Trust check returns false for null or empty allowlists.
     *
     * <pre>
     * Theme: Trusted proxy resolution
     * Test view: Trust check returns false for null or empty allowlists
     * Test conditions: Trusted proxy set is null or empty
     * Test result: isTrustedProxy returns false
     * </pre>
     */
    @Test
    void isTrustedProxyRejectsNullOrEmptyAllowlist() {
        Assertions.assertFalse(
                AccessLogClientIpResolver.isTrustedProxy(ipv4Address(127, 0, 0, 1), null),
                "Null trusted set must be rejected");
        Assertions.assertFalse(
                AccessLogClientIpResolver.isTrustedProxy(ipv4Address(127, 0, 0, 1), Set.of()),
                "Empty trusted set must be rejected");
    }

    /**
     * Canonical remote address forms are accepted when allowlisted.
     *
     * <pre>
     * Theme: Trusted proxy resolution
     * Test view: Canonical remote address forms are accepted when allowlisted
     * Test conditions: Remote address is localhost and allowlist contains loopback IP
     * Test result: isTrustedProxy returns true
     * </pre>
     */
    @Test
    void isTrustedProxyAcceptsCanonicalAddressResolution() {
        Assertions.assertTrue(
                AccessLogClientIpResolver.isTrustedProxy("localhost",
                        Set.of(ipv4Address(127, 0, 0, 1))),
                "Canonical loopback address should be treated as trusted");
    }

    /**
     * Builds an IPv4 string for tests.
     *
     * @param segmentOne first segment
     * @param segmentTwo second segment
     * @param segmentThree third segment
     * @param segmentFour fourth segment
     * @return IPv4 address text
     */
    private static String ipv4Address(final int segmentOne, final int segmentTwo,
            final int segmentThree, final int segmentFour) {
        return String.format(IP_FORMAT, segmentOne, segmentTwo, segmentThree, segmentFour);
    }
}
