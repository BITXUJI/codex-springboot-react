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
    forwarded.addHeader(
        FORWARDED_HEADER,
        String.format(IP_FORMAT, 203, 0, 113, 1) + ", " + String.format(IP_FORMAT, 70, 0, 0, 1));

    Assertions.assertEquals(
        String.format(IP_FORMAT, 203, 0, 113, 1),
        AccessLogFilter.resolveClientIp(forwarded),
        "First forwarded IP should be used");
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

    Assertions.assertEquals(
        String.format(IP_FORMAT, 203, 0, 113, 2),
        AccessLogFilter.resolveClientIp(forwarded),
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

    Assertions.assertEquals(
        String.format(IP_FORMAT, 10, 0, 0, 2),
        AccessLogFilter.resolveClientIp(direct),
        "Remote address should be used when no forwarded header exists");
  }
}
