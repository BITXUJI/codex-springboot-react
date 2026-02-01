package com.example.demo.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link LogSanitizer} body masking. */
class LogSanitizerTest {
  /** JSON content type used by tests. */
  private static final String JSON_CONTENT = "application/json";

  /** Form content type used by tests. */
  private static final String FORM_CONTENT = "application/x-www-form-urlencoded";

  /** Text content type used by tests. */
  private static final String TEXT_CONTENT = "text/plain";

  /** Vendor JSON content type used by tests. */
  private static final String CT_JSON_VENDOR = "application/vnd.test+json";

  /**
   * JSON sensitive fields are masked.
   *
   * <pre>
   * Theme: Body masking
   * Test view: JSON sensitive fields are masked
   * Test conditions: JSON body with password and token
   * Test result: Sensitive values are replaced
   * </pre>
   */
  @Test
  void sanitizeBodyMasksJsonFields() {
    final String body = "{\"password\":\"secret\",\"token\":\"abc\"}";

    final String sanitized = LogSanitizer.sanitizeBody(body, JSON_CONTENT);

    Assertions.assertEquals(
        "{\"password\":\"****\",\"token\":\"****\"}",
        sanitized,
        "JSON sensitive fields should be masked");
  }

  /**
   * Form sensitive fields are masked.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Form sensitive fields are masked
   * Test conditions: Form body with token and password
   * Test result: Sensitive values are replaced
   * </pre>
   */
  @Test
  void sanitizeBodyMasksFormFields() {
    final String body = "token=abc&value=1&password=secret";

    final String sanitized = LogSanitizer.sanitizeBody(body, FORM_CONTENT);

    Assertions.assertEquals(
        "token=****&value=1&password=****", sanitized, "Form sensitive fields should be masked");
  }

  /**
   * Vendor JSON sensitive fields are masked.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Vendor JSON sensitive fields are masked
   * Test conditions: Vendor JSON body with token
   * Test result: Sensitive values are replaced
   * </pre>
   */
  @Test
  void sanitizeBodyMasksVendorJsonFields() {
    final String body = "{\"token\":\"abc\"}";

    final String sanitized = LogSanitizer.sanitizeBody(body, CT_JSON_VENDOR);

    Assertions.assertEquals(
        "{\"token\":\"****\"}", sanitized, "Vendor JSON sensitive fields should be masked");
  }

  /**
   * Null bodies are unchanged.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Null bodies are unchanged
   * Test conditions: Null body value
   * Test result: Null is returned
   * </pre>
   */
  @Test
  void sanitizeBodyKeepsNull() {
    Assertions.assertNull(
        LogSanitizer.sanitizeBody(null, JSON_CONTENT), "Null bodies should remain null");
  }

  /**
   * Blank bodies are unchanged.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Blank bodies are unchanged
   * Test conditions: Body contains whitespace
   * Test result: Body remains unchanged
   * </pre>
   */
  @Test
  void sanitizeBodyKeepsBlank() {
    Assertions.assertEquals(
        " ", LogSanitizer.sanitizeBody(" ", JSON_CONTENT), "Blank bodies should remain unchanged");
  }

  /**
   * Unknown content types are not masked.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Unknown content types are not masked
   * Test conditions: text/plain content type
   * Test result: Body remains unchanged
   * </pre>
   */
  @Test
  void sanitizeBodyKeepsUnknownType() {
    final String body = "password=secret";

    Assertions.assertEquals(
        body,
        LogSanitizer.sanitizeBody(body, TEXT_CONTENT),
        "Unknown content types should not be masked");
  }

  /**
   * Null content types are not masked.
   *
   * <pre>
   * Theme: Body masking
   * Test view: Null content types are not masked
   * Test conditions: Null content type with JSON body
   * Test result: Body remains unchanged
   * </pre>
   */
  @Test
  void sanitizeBodyKeepsNullContentType() {
    final String body = "{\"token\":\"abc\"}";

    Assertions.assertEquals(
        body,
        LogSanitizer.sanitizeBody(body, null),
        "Null content type should not trigger masking");
  }
}
