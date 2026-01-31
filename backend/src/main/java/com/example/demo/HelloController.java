package com.example.demo;

import com.example.demo.api.HelloApi;
import com.example.demo.model.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated interface.
 */
@RestController
public class HelloController implements HelloApi {
    @Override
    public ResponseEntity<HelloResponse> getHello() {
        HelloResponse response = new HelloResponse();
        response.setMessage("Hello from Spring Boot");
        return ResponseEntity.ok(response);
    }
}
