package com.example.demo.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.model.ErrorDetails;
import com.example.demo.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Tests for {@link GlobalExceptionHandler} validation paths. */
class GlobalExceptionHandlerValidationTest {
    /** A sample bean for constraint validation. */
    private static final class ValidationTarget {
        /** Sample name value. */
        @NotBlank
        private final String name;

        /**
         * Creates a validation target.
         *
         * @param name sample name
         */
        private ValidationTarget(final String name) {
            this.name = name;
        }

        /**
         * Returns the sample name.
         *
         * @return name value
         */
        private String getName() {
            return name;
        }
    }

    /**
     * Helper method for building MethodParameter instances.
     *
     * @param value ignored value
     */
    public void handleValidation(@SuppressWarnings("unused") final String value) {
        // Intentionally empty; used only for MethodParameter construction.
    }

    /**
     * Validation exceptions include field errors.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Validation exceptions include field errors
     * Test conditions: Binding result contains field errors
     * Test result: Response details include the field errors
     * </pre>
     *
     * @throws NoSuchMethodException when reflection fails
     */
    @Test
    void handleValidationReturnsFieldErrors() throws NoSuchMethodException {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(
                new FieldError("request", "name", "bad", false, null, null, "must not be blank"));

        final Method method = GlobalExceptionHandlerValidationTest.class
                .getDeclaredMethod("handleValidation", String.class);
        final MethodParameter parameter = new MethodParameter(method, 0);
        final MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        final ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();

        final ErrorDetails responseDetails = body.getDetails();
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getFieldErrors()).hasSize(1);
    }

    /**
     * Empty validation results omit details.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Empty validation results omit details
     * Test conditions: Binding result has no errors
     * Test result: Response details are null
     * </pre>
     *
     * @throws NoSuchMethodException when reflection fails
     */
    @Test
    void handleValidationWithNoErrorsOmitsDetails() throws NoSuchMethodException {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        final Method method = GlobalExceptionHandlerValidationTest.class
                .getDeclaredMethod("handleValidation", String.class);
        final MethodParameter parameter = new MethodParameter(method, 0);
        final MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        final ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }

    /**
     * Constraint violations include field errors.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Constraint violations include field errors
     * Test conditions: Validator returns violations
     * Test result: Response details include field errors
     * </pre>
     */
    @Test
    void handleConstraintViolationReturnsFieldErrors() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final ValidationTarget target = new ValidationTarget("");
        final Set<jakarta.validation.ConstraintViolation<ValidationTarget>> violations =
                validator.validate(target);
        assertThat(target.getName()).isEmpty();
        final ConstraintViolationException exception = new ConstraintViolationException(violations);

        final ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();

        final ErrorDetails responseDetails = body.getDetails();
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getFieldErrors()).isNotEmpty();
    }

    /**
     * Empty constraint violations omit details.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Empty constraint violations omit details
     * Test conditions: No constraint violations
     * Test result: Response details are null
     * </pre>
     */
    @Test
    void handleConstraintViolationWithNoErrorsOmitsDetails() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ConstraintViolationException exception =
                new ConstraintViolationException(java.util.Collections.emptySet());
        final ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetails()).isNull();
    }
}
