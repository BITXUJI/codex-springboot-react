package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point for the Spring Boot service.
 * 
 * <pre>
 * Responsibilities:
 * 1) Bootstraps the Spring application.
 * 2) Supports opt-in exit-on-start mode for smoke runs.
 * 3) Stores the last started context for test visibility.
 * </pre>
 */
@SpringBootApplication
public final class DemoApplication {
    /**
     * Stable marker value used by tests that verify instance creation.
     */
    /* default */ static final String INSTANCE_MARKER = "instance";

    /**
     * Tracks the most recently started application context (primarily for tests).
     */
    private static ConfigurableApplicationContext lastContext;

    /**
     * Application entry point.
     * 
     * <pre>
     * Algorithm:
     * 1) Check the app.exitOnStart system property.
     * 2) If enabled, start the context in try-with-resources and close it immediately.
     * 3) Otherwise delegate to run(args) and keep the process alive.
     * </pre>
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        if (Boolean.getBoolean("app.exitOnStart")) {
            try (ConfigurableApplicationContext context = run(args)) {
                context.getId();
            }
            return;
        }
        run(args);
    }

    /**
     * Starts the Spring application context.
     * 
     * <pre>
     * Algorithm:
     * 1) Delegate startup to SpringApplication.run.
     * 2) Cache the returned context in lastContext.
     * 3) Return the context so callers can manage lifecycle if needed.
     * </pre>
     *
     * @param args command-line arguments
     * @return running application context
     */
    public static ConfigurableApplicationContext run(final String... args) {
        final ConfigurableApplicationContext context =
                SpringApplication.run(DemoApplication.class, args);
        lastContext = context;
        return context;
    }

    /**
     * Returns the most recently started application context.
     * 
     * <pre>
     * Algorithm:
     * 1) Read the static lastContext cache.
     * 2) Return null when startup has not happened yet.
     * </pre>
     *
     * @return last started application context, or null if none
     */
    /* default */ static ConfigurableApplicationContext getLastContext() {
        return lastContext;
    }

    /**
     * Prevent instantiation of the application class.
     * 
     * <pre>
     * Design note:
     * 1) Startup APIs are exposed as static methods.
     * 2) A private constructor prevents accidental object creation.
     * </pre>
     */
    private DemoApplication() {
        // Intentionally empty.
    }

}
