package com.example.demo;

import com.example.demo.api.HelloApi;
import com.example.demo.model.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated interface.
 * 
 * <pre>
 * Responsibilities:
 * 1) Bind the generated API contract to a Spring REST controller.
 * 2) Build HelloResponse payloads with a deterministic default greeting.
 * 3) Return HTTP 200 responses for successful hello requests.
 * </pre>
 */
@RestController
public class HelloController implements HelloApi {
    /** Default greeting returned by the API. */
    private final String message;

    /**
     * Returns a hello message.
     * 
     * <pre>
     * Algorithm:
     * 1) Create a new HelloResponse instance.
     * 2) Populate the message field from the configured default value.
     * 3) Wrap the payload with ResponseEntity.ok(...).
     * </pre>
     *
     * @return the hello response payload
     */
    @Override
    public ResponseEntity<HelloResponse> getHello() {
        final HelloResponse response = new HelloResponse();
        response.setMessage(message);
        return ResponseEntity.ok(response);
    }

    /**
     * Default constructor.
     * 
     * <pre>
     * Initialization:
     * 1) Assign the fixed default greeting used by getHello().
     * 2) Keep state immutable after controller creation.
     * </pre>
     */
    public HelloController() {
        this.message = "Hello from Spring Boot";
    }
}
