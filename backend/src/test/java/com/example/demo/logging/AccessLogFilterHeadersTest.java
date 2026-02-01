package com.example.demo.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests for header sanitization. */
class AccessLogFilterHeadersTest {
  /** API path used by tests. */
  private static final String PATH_API = "/api/hello";

  /** Header key used by tests. */
  private static final String HEADER_TEST = "X-Test";

  /** HTTP GET method name. */
  private static final String METHOD_GET = "GET";

  /**
   * Null header names result in empty map.
   *
   * <pre>
   * Theme: Header sanitization
   * Test view: Null header names result in empty map
   * Test conditions: getHeaderNames returns null
   * Test result: Sanitized map is empty
   * </pre>
   */
  @Test
  void sanitizeRequestHeadersHandlesNullNames() {
    final HttpServletRequest noHeaders =
        new MockHttpServletRequest(METHOD_GET, PATH_API) {
          @Override
          public Enumeration<String> getHeaderNames() {
            return null;
          }
        };

    final Map<String, String> headers = AccessLogFilter.sanitizeHeaders(noHeaders);

    Assertions.assertEquals(0, headers.size(), "Empty header names should yield empty map");
  }

  /**
   * Null header values are handled.
   *
   * <pre>
   * Theme: Header sanitization
   * Test view: Null header values are handled
   * Test conditions: getHeaders returns null
   * Test result: Header value is stored as empty string
   * </pre>
   */
  @Test
  void sanitizeRequestHeadersHandlesNullValues() {
    final HttpServletRequest nullValues =
        new MockHttpServletRequest(METHOD_GET, PATH_API) {
          @Override
          public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(List.of(HEADER_TEST));
          }

          @Override
          public Enumeration<String> getHeaders(final String name) {
            return null;
          }
        };

    final Map<String, String> headers = AccessLogFilter.sanitizeHeaders(nullValues);

    Assertions.assertEquals(
        "", headers.get(HEADER_TEST), "Null header values should be stored as empty strings");
  }

  /**
   * Null response header values are handled.
   *
   * <pre>
   * Theme: Header sanitization
   * Test view: Null response header values are handled
   * Test conditions: getHeader returns null
   * Test result: Header value is stored as empty string
   * </pre>
   */
  @Test
  void sanitizeResponseHeadersHandlesNullValues() {
    final HttpServletResponse nullResponse =
        new MockHttpServletResponse() {
          @Override
          public Collection<String> getHeaderNames() {
            return List.of(HEADER_TEST);
          }

          @Override
          public String getHeader(final String name) {
            return null;
          }
        };

    final Map<String, String> headers = AccessLogFilter.sanitizeHeaders(nullResponse);

    Assertions.assertEquals(
        "",
        headers.get(HEADER_TEST),
        "Null response header values should be stored as empty strings");
  }
}
