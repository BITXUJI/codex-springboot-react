package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Entry point for the Spring Boot service. */
@SpringBootApplication
public final class DemoApplication {
    /**
     * Tracks the most recently started application context (primarily for tests).
     */
    private static ConfigurableApplicationContext lastContext;

    /**
     * Application entry point.
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
     * Creates a new application instance for test coverage.
     *
     * @return new application instance
     */
    /* default */ static DemoApplication createForTest() {
        return new DemoApplication();
    }

    /**
     * Returns the most recently started application context.
     *
     * @return last started application context, or null if none
     */
    /* default */ static ConfigurableApplicationContext getLastContext() {
        return lastContext;
    }

    /** Prevent instantiation of the application class. */
    private DemoApplication() {
        // Intentionally empty.
    }

    /**
     * Returns a marker string for instance access.
     *
     * @return marker string
     */
    /* default */ String instanceMarker() {
        return "instance";
    }
}
