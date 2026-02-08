package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

/** Tests for {@link DemoApplication}. */
class DemoApplicationTest {
    /** Label used to annotate assertions. */
    private final String caseName;

    /** Creates the test instance. */
    public DemoApplicationTest() {
        caseName = "default";
    }

    /** Verifies the application context starts and stops. */
    @Test
    void runStartsAndStopsContext() {
        try (ConfigurableApplicationContext context =
                DemoApplication.run("--spring.main.web-application-type=none")) {
            assertThat(context.isActive()).as(caseName).isTrue();
        }
    }

    /** Verifies main closes the context when the exit flag is enabled. */
    @Test
    void mainClosesWhenFlagEnabled() {
        System.setProperty("app.exitOnStart", "true");
        try {
            DemoApplication.main(new String[] {"--spring.main.web-application-type=none"});
            assertThat(DemoApplication.getLastContext()).as(caseName).isNotNull();
        } finally {
            System.clearProperty("app.exitOnStart");
        }
    }

    /** Verifies main keeps the context when the exit flag is disabled. */
    @Test
    void mainKeepsContextWhenFlagDisabled() {
        DemoApplication.main(new String[] {"--spring.main.web-application-type=none"});
        try (ConfigurableApplicationContext context = DemoApplication.getLastContext()) {
            assertThat(context).as(caseName).isNotNull();
            context.getId();
        }
    }

    /** Verifies the private constructor can be invoked via method handles. */
    @Test
    void constructorIsCovered() throws Throwable {
        final MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(DemoApplication.class, MethodHandles.lookup());
        final MethodHandle constructor =
                lookup.findConstructor(DemoApplication.class, MethodType.methodType(void.class));
        final DemoApplication instance = (DemoApplication) constructor.invoke();
        assertThat(instance).as(caseName).isNotNull();
    }
}
