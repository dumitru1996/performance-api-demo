package org.example.ui.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Playwright / browser configuration bound from {@code test.framework.ui.*} in application.yml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "test.framework.ui")
public class PlaywrightProperties {

    private String baseUrl;

    private String browser;

    private boolean headless;

    private long pageLoadTimeout;

    private List<String> customProperties;

    private List<String> headlessArguments;

    private Slow slow = new Slow();

    @Data
    public static class Slow {
        private int motion;
        private int timeout;
    }

}

