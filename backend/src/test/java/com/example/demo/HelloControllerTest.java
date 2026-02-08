package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Verifies the hello endpoint returns a message. */
@SpringBootTest
class HelloControllerTest {
    /** Spring web application context used to configure MockMvc. */
    private final WebApplicationContext context;

    /** MockMvc instance used for endpoint assertions. */
    private MockMvc mockMvc;

    /**
     * Creates the test instance with Spring's web context.
     *
     * @param context web application context
     */
    public HelloControllerTest(final WebApplicationContext context) {
        this.context = context;
    }

    /** Builds MockMvc before each test. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /**
     * Ensures the hello endpoint returns the expected payload.
     *
     * @throws Exception when the request fails
     */
    @Test
    void returnsHelloMessage() throws Exception {
        Assertions.assertThat(mockMvc).isNotNull();
        mockMvc.perform(get("/api/hello")).andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Hello from Spring Boot\"}"));
    }

    /**
     * Ensures unknown API paths return a standardized not found payload.
     *
     * @throws Exception when the request fails
     */
    @Test
    void returnsNotFoundForUnknownApiPath() throws Exception {
        Assertions.assertThat(mockMvc).isNotNull();
        mockMvc.perform(get("/api/e2e-not-found-example")).andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Not found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path")
                        .value("/api/e2e-not-found-example"));
    }
}
