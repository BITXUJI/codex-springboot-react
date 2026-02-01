package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Tests for {@link ErrorResponseFactory} mapping helpers. */
class ErrorResponseFactoryMappingTest {
    /** Sample field name used in tests. */
    private static final String FIELD_NAME = "name";
    /** Sample field message used in tests. */
    private static final String FIELD_MESSAGE = "message";

    /**
     * Rejected values are mapped.
     *
     * <pre>
     * Theme: Error mapping
     * Test view: Rejected values are mapped
     * Test conditions: Null and non-null rejected values
     * Test result: JsonNullable reflects the inputs
     * </pre>
     */
    @Test
    void mapsRejectedValueToFieldError() {
        final com.example.demo.model.FieldError nullValue =
                ErrorResponseFactory.toFieldError(FIELD_NAME, FIELD_MESSAGE, null);
        assertThat(nullValue.getRejectedValue().isPresent()).isFalse();

        final com.example.demo.model.FieldError withValue =
                ErrorResponseFactory.toFieldError(FIELD_NAME, FIELD_MESSAGE, "bad");
        assertThat(withValue.getRejectedValue().isPresent()).isTrue();
        assertThat(withValue.getRejectedValue().get()).isEqualTo("bad");
    }

    /**
     * Default message is used when missing.
     *
     * <pre>
     * Theme: Error mapping
     * Test view: Default message is used when missing
     * Test conditions: Null message provided
     * Test result: Message falls back to "Invalid value"
     * </pre>
     */
    @Test
    void usesDefaultMessageWhenMissing() {
        final com.example.demo.model.FieldError error =
                ErrorResponseFactory.toFieldError(FIELD_NAME, null, "bad");

        assertThat(error.getMessage()).isEqualTo("Invalid value");
    }

    /**
     * Status codes map to error codes.
     *
     * <pre>
     * Theme: Error mapping
     * Test view: Status codes map to error codes
     * Test conditions: Representative HTTP statuses
     * Test result: Error codes match expected values
     * </pre>
     */
    @Test
    void mapsStatusToErrorCode() {
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.UNAUTHORIZED))
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.FORBIDDEN))
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.NOT_FOUND))
                .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.CONFLICT))
                .isEqualTo(ErrorCode.CONFLICT);
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.BAD_REQUEST))
                .isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ErrorResponseFactory.mapStatusToCode(HttpStatus.SERVICE_UNAVAILABLE))
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}
