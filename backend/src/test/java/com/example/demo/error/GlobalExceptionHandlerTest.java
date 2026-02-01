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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

/** Tests for {@link GlobalExceptionHandler}. */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.LawOfDemeter", "PMD.TooManyMethods"})
class GlobalExceptionHandlerTest {
    /** Trace id used for test requests. */
    private static final String TRACE_ID = "trace-abc";

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
     * Application exceptions map to configured status.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Application exceptions map to configured status
     * Test conditions: AppException with BAD_REQUEST
     * Test result: Response status and code reflect the exception
     * </pre>
     */
    @Test
    void handleAppExceptionReturnsConfiguredStatus() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");
        request.addHeader("X-Request-Id", TRACE_ID);

        final ErrorDetails details = new ErrorDetails();
        details.putAdditionalProperty("info", "value");
        final AppException exception = new AppException(ErrorCode.BAD_REQUEST, "Bad request",
                HttpStatus.BAD_REQUEST, details);

        final ResponseEntity<ErrorResponse> response =
                handler.handleAppException(exception, request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.name());
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

        final Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidation",
                String.class);
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
        final Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidation",
                String.class);
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

    /**
     * Malformed payloads return bad request.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Malformed payloads return bad request
     * Test conditions: HttpMessageNotReadableException thrown
     * Test result: Response status is BAD_REQUEST
     * </pre>
     */
    @Test
    void handleNotReadableReturnsBadRequest() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleNotReadable(
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Response status exceptions map to codes.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Response status exceptions map to codes
     * Test conditions: ResponseStatusException with NOT_FOUND
     * Test result: Response status and code match NOT_FOUND
     * </pre>
     */
    @Test
    void handleResponseStatusReturnsMappedCode() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing"), request);

        final ErrorResponse body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    /**
     * Missing reason uses default phrase.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Missing reason uses default phrase
     * Test conditions: ResponseStatusException without reason
     * Test result: Message equals the default reason phrase
     * </pre>
     */
    @Test
    void handleResponseStatusUsesDefaultReason() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response = handler
                .handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND), request);

        final ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    /**
     * Not found handler returns 404.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Not found handler returns 404
     * Test conditions: NoHandlerFoundException is raised
     * Test result: Response status is NOT_FOUND
     * </pre>
     */
    @Test
    void handleNotFoundReturnsNotFound() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/missing");

        final NoHandlerFoundException exception =
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders());
        final ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Unhandled exceptions map to internal errors.
     *
     * <pre>
     * Theme: Exception mapping
     * Test view: Unhandled exceptions map to internal errors
     * Test conditions: Runtime exception is thrown
     * Test result: Response status is INTERNAL_SERVER_ERROR
     * </pre>
     */
    @Test
    void handleUnhandledReturnsInternalServerError() {
        final GlobalExceptionHandler handler = new GlobalExceptionHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/hello");

        final ResponseEntity<ErrorResponse> response =
                handler.handleUnhandled(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
