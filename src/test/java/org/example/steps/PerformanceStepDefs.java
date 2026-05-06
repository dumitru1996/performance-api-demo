package org.example.steps;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.example.ui.browser.BrowserManager;
import org.example.ui.config.PlaywrightProperties;

import java.util.Map;


@Slf4j
@RequiredArgsConstructor
public class PerformanceStepDefs {

    private final BrowserManager browserManager;
    private final PlaywrightProperties properties;


    @When("user navigate to External Cooperation Page through menu for performance validation")
    public void validateExternalCooperationPagePerformance() {
        Page playwrightPage = browserManager.getPage();

        playwrightPage.locator("//*[@id=\"mainmenu\"]/ul/li[3]/a").click();
        playwrightPage.locator("//*[@id=\"mainmenu\"]/ul/li[3]/div/div/div[1]/a")
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10_000));
        playwrightPage.locator("//*[@id=\"mainmenu\"]/ul/li[3]/div/div/div[1]/a").click();

        playwrightPage.waitForLoadState(LoadState.NETWORKIDLE);
        assertOnPage(playwrightPage, "//a[contains(@title,'home')]");
        playwrightPage.waitForLoadState(LoadState.LOAD);

        executeDefaultPerformanceScripts(playwrightPage, "cooperare-externa.nspx");
    }

    @When("validate page performance on reload")
    public void pageReloadPerformanceValidation() {
        Page playwrightPage = browserManager.getPage();

        playwrightPage.evaluate("performance.clearResourceTimings()");
        initWebVitalsWithImport(playwrightPage);
        playwrightPage.reload();
        playwrightPage.waitForLoadState(LoadState.NETWORKIDLE);

        assertOnPage(playwrightPage, "//a[contains(@title,'home')]");

        playwrightPage.waitForLoadState(LoadState.LOAD);
        playwrightPage.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Map<String, Object> vitals = (Map<String, Object>) playwrightPage.evaluate("() => window.__vitals");

        executeDefaultPerformanceScripts(playwrightPage, "parlament.md");
        log.info("Web Vitals on Reload: {}", vitals);
    }

    @When("page performance validation on direct navigation")
    public void pagePerformanceValidationOnDirectNavigation() throws InterruptedException {
        Page playwrightPage = browserManager.getPage();

        playwrightPage.evaluate("performance.clearResourceTimings()");
        initWebVitals(playwrightPage);
        Thread.sleep(3_000);
        browserManager.getPage().navigate(
                properties.getBaseUrl(),
                new Page.NavigateOptions()
                        .setTimeout(properties.getPageLoadTimeout())
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
        );
        playwrightPage.waitForLoadState(LoadState.NETWORKIDLE);
        assertOnPage(playwrightPage, "//a[contains(@title,'home')]");
        playwrightPage.waitForLoadState(LoadState.LOAD);


        Map<String, Object> vitals = (Map<String, Object>) playwrightPage.evaluate("() => window.__vitals");

        executeDefaultPerformanceScripts(playwrightPage, "parlament.md");
        log.info("CLS: new script {}ms", vitals);
    }

    @When("measure search duration")
    public void measureSeachDuration() throws InterruptedException {
        Page playwrightPage = browserManager.getPage();
        playwrightPage.evaluate("performance.clearResourceTimings()");

        playwrightPage.locator("//label/input[contains(@name,'q')]").fill("test");
        playwrightPage.locator("//img[contains(@alt,'parliament of moldova search')]").click();

        playwrightPage.evaluate("performance.mark('search-start')");
        assertOnPage(playwrightPage, "//span[contains(@id,'search1')]//li/h2");
        playwrightPage.waitForLoadState(LoadState.LOAD);
        playwrightPage.waitForLoadState(LoadState.NETWORKIDLE);

        playwrightPage.evaluate("performance.mark('search-end'); ");

        playwrightPage.evaluate("""
                performance.measure(
                  'search-roundtrip',  // Measurement name
                  'search-start',      // Starting mark
                  'search-end'         // Ending mark
                );
                """);

        Thread.sleep(1_000);

        Object elapsed = playwrightPage.evaluate(
                """
                         () => { 
                            const markEntry = performance.getEntriesByName('search-start')[0]; 
                            if (!markEntry) return -1; 
                            const markTime = markEntry.startTime; 
                            const navEntries = performance.getEntriesByType('navigation'); 
                            const resEntries = performance.getEntriesByType('resource'); 
                            const allEntries = [...navEntries, ...resEntries]; 
                            const lastEndTime = Math.max(...allEntries.map(e => e.responseEnd)); 
                            return lastEndTime - markTime; }
                        """
        );

        Object roundTrip = playwrightPage.evaluate("""
                () => {
                    const measure = performance.getEntriesByName('search-roundtrip')[0];
                    return measure.duration;                
                }
                """);

        log.info("Save action round-trip: {}ms", roundTrip);
        log.info("Save action round-trip: {}ms", elapsed);


//        measureCoreWebMetrics(playwrightPage);

    }

    private void assertOnPage(Page playwrightPage, String locator) {
        Locator element = playwrightPage.locator(locator);
        element.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10_000));
        Assertions.assertThat(element.isVisible()).isTrue();
    }

    private void executeDefaultPerformanceScripts(Page playwrightPage, String pageName) {
        Object totalDuration = playwrightPage.evaluate("""
                () => {
                  const navs = performance.getEntriesByType('navigation');
                  if (!navs.length) return null;
                  const nav = navs[0];
                  return nav.loadEventEnd - nav.startTime;
                }
                """);

        Object pageDuration = playwrightPage.evaluate(String.format("""
                () => {
                  const resources = performance.getEntriesByType("navigation");
                  const appEntry = resources.find(r => r.name.includes("%s"));
                  if (!appEntry) return -1;
                  const start = appEntry.startTime;
                  const lastEnd = Math.max(...resources.map(r => r.responseEnd));
                  return lastEnd - start;
                }
                """, pageName));

        Object totalDownloadDuration = playwrightPage.evaluate("""
                () => {
                  const resources = performance.getEntriesByType('resource');
                  if (!resources.length) return null;
                  const firstStart = Math.min(...resources.map(r => r.startTime));
                  const lastEnd = Math.max(...resources.map(r => r.responseEnd));
                  return lastEnd - firstStart;
                }
                """);

        log.info("Server processing time + network transfer + DOM rendering : {}ms", totalDuration);
        log.info("Page render time on load Home Page: {}ms", pageDuration);
        log.info("Total download duration : {}ms", totalDownloadDuration);
    }

    private void measureCoreWebMetrics(Page playwrightPage) {
        // Get LCP (Largest Contentful Paint)
        Object lcp = playwrightPage.evaluate("""
                    () => {
                      // Get navigation and paint entries
                      const nav = performance.getEntriesByType('navigation')[0];
                      const paints = performance.getEntriesByType('paint');
                
                      // LCP is typically the later of these
                      let lcpTime = nav ? nav.loadEventEnd : 0;
                      paints.forEach(p => {
                        if (p.name === 'first-contentful-paint') {
                          lcpTime = Math.max(lcpTime, p.startTime);
                        }
                      });
                
                      return lcpTime;
                    }
                """);

        log.info("LCP: {}ms", lcp);

        Object cls = playwrightPage.evaluate("""
                    () => {
                      let clsValue = 0;
                
                      const observer = new PerformanceObserver((list) => {
                        for (const entry of list.getEntries()) {
                          if (!entry.hadRecentInput) {
                            clsValue += entry.value;
                          }
                        }
                      });
                
                      observer.observe({entryTypes: ['layout-shift']});
                
                      // Wait for all layout shifts
                      setTimeout(() => observer.disconnect(), 3000);
                
                      return clsValue;
                    }
                """);

        log.info("CLS: {}", cls);
    }

    public void initWebVitals(Page playwrightPage) {
        playwrightPage.addInitScript("""
                (() => {
                  window.__vitals = {};
                
                  function storeMetric(name, value) {
                    window.__vitals[name] = value;
                  }
                
                  // CLS
                  let clsValue = 0;
                  let sessionValue = 0;
                  let sessionEntries = [];
                
                  new PerformanceObserver((list) => {
                    for (const entry of list.getEntries()) {
                      if (entry.hadRecentInput) continue;
                
                      const first = sessionEntries[0];
                      const last = sessionEntries[sessionEntries.length - 1];
                
                      if (
                        sessionValue &&
                        entry.startTime - last.startTime < 1000 &&
                        entry.startTime - first.startTime < 5000
                      ) {
                        sessionValue += entry.value;
                        sessionEntries.push(entry);
                      } else {
                        sessionValue = entry.value;
                        sessionEntries = [entry];
                      }
                
                      clsValue = Math.max(clsValue, sessionValue);
                      storeMetric('CLS', clsValue);
                    }
                  }).observe({ type: 'layout-shift', buffered: true });
                
                  // LCP
                  new PerformanceObserver((list) => {
                    const entries = list.getEntries();
                    const lastEntry = entries[entries.length - 1];
                    storeMetric('LCP', lastEntry.renderTime || lastEntry.loadTime);
                  }).observe({ type: 'largest-contentful-paint', buffered: true });
                
                  // FCP
                  new PerformanceObserver((list) => {
                    const entry = list.getEntries()[0];
                    storeMetric('FCP', entry.startTime);
                  }).observe({ type: 'paint', buffered: true });
                
                  // INP (simplified)
                  new PerformanceObserver((list) => {
                    for (const entry of list.getEntries()) {
                      storeMetric('INP', entry.duration);
                    }
                  }).observe({ type: 'event', buffered: true, durationThreshold: 40 });
                
                  // TTFB
                  new PerformanceObserver((list) => {
                    const entry = list.getEntries()[0];
                    storeMetric('TTFB', entry.responseStart);
                  }).observe({ type: 'navigation', buffered: true });
                
                })();
                """);
    }

    public void initWebVitalsWithImport(Page playwrightPage) {
        playwrightPage.addInitScript("""
                const script = document.createElement('script');
                script.src = 'https://unpkg.com/web-vitals@3/dist/web-vitals.iife.js';
                script.onload = () => {
                  webVitals.onCLS(m => window.__vitals.CLS = m.value);
                  webVitals.onLCP(m => window.__vitals.LCP = m.value);
                  webVitals.onINP(m => window.__vitals.INP = m.value);
                  webVitals.onFCP(m => window.__vitals.FCP = m.value);
                  webVitals.onTTFB(m => window.__vitals.TTFB = m.value);
                };
                document.head.appendChild(script);
                
                window.__vitals = {};
                """);
    }

}

