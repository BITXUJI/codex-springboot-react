package com.example.demo;

import com.example.demo.api.HelloApi;
import com.example.demo.model.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Implements the OpenAPI-generated interface. */
@RestController
public class HelloController implements HelloApi {
    /** Default greeting returned by the API. */
    private final String message;

    /**
     * Returns a hello message.
     *
     * @return the hello response payload
     */
    @Override
    public ResponseEntity<HelloResponse> getHello() {
        final HelloResponse response = new HelloResponse();
        response.setMessage(message);
        return ResponseEntity.ok(response);
    }

    /** Default constructor. */
    public HelloController() {
        this.message = "Hello from Spring Boot";
    }
}
