package org.example.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Wires the Cucumber step definitions into the Spring application context.
 * <p>
 * {@code @CucumberContextConfiguration} is required by {@code cucumber-spring}
 * so that all step / hook classes can use {@code @Autowired}.
 * </p>
 */
@CucumberContextConfiguration
@SpringBootTest(classes = TestFrameworkApplication.class)
public class CucumberSpringConfiguration {
}

