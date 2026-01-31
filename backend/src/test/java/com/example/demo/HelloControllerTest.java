package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Verifies the hello endpoint returns a message. */
@SpringBootTest
class HelloControllerTest {
  /** Spring web application context used to configure MockMvc. */
  @Autowired private WebApplicationContext applicationContext;

  private MockMvc mockMvc;

  /** Builds MockMvc before each test. */
  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  /**
   * Ensures the hello endpoint returns the expected payload.
   *
   * @throws Exception when the request fails
   */
  @Test
  void returnsHelloMessage() throws Exception {
    mockMvc
        .perform(get("/api/hello"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"message\":\"Hello from Spring Boot\"}"));
  }
}
