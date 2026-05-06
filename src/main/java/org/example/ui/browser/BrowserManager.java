package org.example.ui.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.ui.config.PlaywrightProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Manages the Playwright browser lifecycle for a single Cucumber scenario.
 * <p>
 * Scoped to {@code cucumber-glue} so a fresh instance is created per scenario.
 * Supports local launch and remote Playwright server connections.
 * </p>
 */
@Slf4j
@Component
@Scope("cucumber-glue")
public class BrowserManager implements BaseBrowser {

    private final PlaywrightProperties properties;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    @Setter
    @Getter
    private Page page;

    public BrowserManager(PlaywrightProperties properties) {
        this.properties = properties;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initialises the Playwright runtime, browser and page.
     * <p>
     * If the environment / system property {@code PLAYWRIGHT_SERVER_URL} is set, the
     * browser connects to a remote Playwright server (pipeline / grid mode).
     * Otherwise a local browser is launched.
     * </p>
     */
    @Override
    public void init() {
        log.info("Initialising browser: {}, headless: {}", properties.getBrowser(), properties.isHeadless());

        playwright = Playwright.create();
        browser = BrowserFactory.createBrowser(playwright, properties.getBrowser(), null, properties);

        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
        page = context.newPage();
        page.setDefaultTimeout(properties.getSlow().getTimeout());

    }

    @Override
    public void close() {
        log.info("Closing browser resources");
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

}

