package org.example.ui.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import org.example.ui.config.PlaywrightProperties;

import static com.microsoft.playwright.BrowserType.LaunchOptions;

/**
 * Factory responsible for creating a {@link Browser} instance based on the configured browser name.
 */
public class BrowserFactory {

    private BrowserFactory() {
    }

    /**
     * Creates and returns a {@link Browser} instance.
     * <p>
     * If {@code remoteAddress} is not {@code null}, connects to a remote Chromium instance.
     * Otherwise, launches a local browser of the requested type.
     * </p>
     */
    public static Browser createBrowser(Playwright playwright,
                                        String browserName,
                                        String remoteAddress,
                                        PlaywrightProperties properties) {
        if (remoteAddress != null) {
            return playwright.chromium().connect(remoteAddress);
        }
        return switch (browserName.toLowerCase()) {
            case "chromium" -> playwright.chromium().launch(options(properties));
            case "firefox" -> playwright.firefox().launch(options(properties));
            case "webkit" -> playwright.webkit().launch(options(properties));
            default -> throw new IllegalArgumentException("Unsupported browser: " + browserName);
        };
    }

    private static LaunchOptions options(PlaywrightProperties props) {
        LaunchOptions options = new LaunchOptions();
        final boolean headless = props.isHeadless();
        if (headless) {
            options.setArgs(props.getHeadlessArguments());
        }
        if (props.getCustomProperties() != null) {
            options.setArgs(props.getCustomProperties());
        }

        options.setSlowMo(props.getSlow().getMotion());
//        List<String> args = new ArrayList<>();
//        args.add("--disable-cache");
//        args.add("--disable-application-cache");
//        args.add("--aggressive-cache-discard");
//        args.add("--disk-cache-size=0");
//        options.setArgs(args);
        return options.setHeadless(headless);
    }
}

