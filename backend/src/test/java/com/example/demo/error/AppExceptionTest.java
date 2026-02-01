package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.model.ErrorDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Tests for {@link AppException}. */
@SuppressWarnings("PMD.LawOfDemeter")
class AppExceptionTest {
    /**
     * Constructor keeps null details.
     *
     * <pre>
     * Theme: Exception details
     * Test view: Constructor keeps null details
     * Test conditions: Details argument is null
     * Test result: getDetails returns null
     * </pre>
     */
    @Test
    void constructorWithoutDetailsKeepsNull() {
        final AppException exception =
                new AppException(ErrorCode.BAD_REQUEST, "Bad request", HttpStatus.BAD_REQUEST);

        assertThat(exception.getDetails()).isNull();
    }

    /**
     * Details are defensively copied.
     *
     * <pre>
     * Theme: Exception details
     * Test view: Details are defensively copied
     * Test conditions: Details include additional properties
     * Test result: Copy differs from source but retains properties
     * </pre>
     */
    @Test
    void constructorCopiesDetails() {
        final ErrorDetails details = new ErrorDetails();
        details.setFieldErrors(null);
        details.putAdditionalProperty("info", "value");

        final AppException exception = new AppException(ErrorCode.BAD_REQUEST, "Bad request",
                HttpStatus.BAD_REQUEST, details);

        final ErrorDetails copy = exception.getDetails();
        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(details);
        assertThat(copy.getAdditionalProperties()).containsEntry("info", "value");
    }

    /**
     * Missing additional properties are handled.
     *
     * <pre>
     * Theme: Exception details
     * Test view: Missing additional properties are handled
     * Test conditions: Details have no extra properties
     * Test result: Copy has null additional properties
     * </pre>
     */
    @Test
    void constructorHandlesMissingAdditionalProperties() {
        final ErrorDetails details = new ErrorDetails();
        details.setFieldErrors(null);

        final AppException exception = new AppException(ErrorCode.BAD_REQUEST, "Bad request",
                HttpStatus.BAD_REQUEST, details);

        final ErrorDetails copy = exception.getDetails();
        assertThat(copy).isNotNull();
        assertThat(copy.getAdditionalProperties()).isNull();
    }
}
