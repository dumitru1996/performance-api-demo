package org.example.hooks;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ui.browser.BrowserManager;
import org.example.ui.config.PlaywrightProperties;

@Slf4j
@RequiredArgsConstructor
public class UiHooks {

    private final BrowserManager browserManager;
    private final PlaywrightProperties properties;

    @Before(order = 19, value = "@UI")
    public void initBrowser() {
        log.info("Initialising browser and navigating to base URL: {}", properties.getBaseUrl());
        browserManager.init();
    }

    @After(order = 1, value = "@UI")
    public void closeBrowser() {
        browserManager.close();
    }
}
