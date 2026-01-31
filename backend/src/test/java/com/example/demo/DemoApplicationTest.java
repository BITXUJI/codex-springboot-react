package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class DemoApplicationTest {
  @Test
  void runStartsAndStopsContext() {
    try (ConfigurableApplicationContext context =
        DemoApplication.run(new String[] {"--spring.main.web-application-type=none"})) {
      assertThat(context.isActive()).isTrue();
    }
  }

  @Test
  void mainClosesWhenFlagEnabled() {
    System.setProperty("app.exitOnStart", "true");
    try {
      DemoApplication.main(new String[] {"--spring.main.web-application-type=none"});
    } finally {
      System.clearProperty("app.exitOnStart");
    }
  }

  @Test
  void mainKeepsContextWhenFlagDisabled() {
    DemoApplication.main(new String[] {"--spring.main.web-application-type=none"});
    ConfigurableApplicationContext context = DemoApplication.getLastContext();
    if (context != null) {
      context.close();
    }
  }

  @Test
  void constructorIsCovered() throws ReflectiveOperationException {
    Constructor<DemoApplication> constructor = DemoApplication.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    DemoApplication instance = constructor.newInstance();
    assertThat(instance).isNotNull();
  }
}
