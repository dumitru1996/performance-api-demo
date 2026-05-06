package org.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot entry point for the test framework.
 * <p>
 * {@code cucumber-spring} picks this up automatically when scanning for a
 * {@code @SpringBootApplication} class on the classpath.
 * </p>
 */
@SpringBootApplication(scanBasePackages = "org.example")
@EnableConfigurationProperties
public class TestFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestFrameworkApplication.class, args);
    }
}

