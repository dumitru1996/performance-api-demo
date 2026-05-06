# UI Performance Testing with Playwright

**Scope:** Demonstrate practical work with the Performance API and Web Vitals, including core UI performance thresholds and their explanations. Defining Pages and Locators is **Out of Scope**, as a result this aspect of the repo was simplified. 

This guide helps you measure and validate how fast your web application actually feels to users, covering initial page loads, navigation performance, and user interactions.

## Table of Contents

- [What This Is](#what-this-is)
- [Quick Start](#quick-start)
  - [Measure Performance Manually](#measure-performance-manually)
  - [Automate with Playwright](#automate-with-playwright)
- [What You Need to Measure](#what-you-need-to-measure)
  - [Initial Screen Load](#initial-screen-load)
  - [Navigation Between Pages](#navigation-between-pages)
  - [Action Execution (Create/Edit/Save)](#action-execution-createeditsave)
- [Code Breakdown](#code-breakdown)
- [Automating with Playwright](#automating-with-playwright)
  - [Understanding page.evaluate()](#understanding-pageevaluate)
  - [Understanding window.__vitals](#understanding-window__vitals)
  - [Basic Performance Measurement](#basic-performance-measurement)
  - [Real-World Examples](#real-world-examples)
    - [Measuring Initial Page Load](#measuring-initial-page-load)
    - [Measuring Page Reload](#measuring-page-reload)
    - [Measuring Action Execution (Search)](#measuring-action-execution-search)
- [Web Vitals – Core Metrics](#web-vitals--core-metrics)
  - [CLS (Cumulative Layout Shift)](#cls-cumulative-layout-shift)
  - [LCP (Largest Contentful Paint)](#lcp-largest-contentful-paint)
  - [FCP (First Contentful Paint)](#fcp-first-contentful-paint)
  - [INP (Interaction to Next Paint)](#inp-interaction-to-next-paint)
  - [TTFB (Time To First Byte)](#ttfb-time-to-first-byte)
  - [Complete Web Vitals Implementation](#complete-web-vitals-implementation)
- [Simplified Approach: Using the Web Vitals Library](#simplified-approach-using-the-web-vitals-library)
  - [Why Use a Library?](#why-use-a-library)
  - [Implementation](#implementation)
  - [Comparison](#comparison)
  - [Usage Example](#usage-example)
- [References](#references)
  - [Official Documentation](#official-documentation)
  - [Tools](#tools)
  - [Specifications](#specifications)

## What This Is

This guide demonstrate how to measure and validate how fast a web application actually feels to users. Main flows:

- **Initial page load** – How long until the app is usable when first opened
- **Navigation** – How quickly pages load when clicking through the app  
- **User actions** – Response time for buttons, forms, and other interactions

You'll learn to identify bottlenecks, automate performance checks, and interpret the metrics that matter.

## Quick Start

### Measure Performance Manually

Open Chrome DevTools (F12) and use these three tools:

1. **Network tab** – See what's downloading and how long it takes
2. **Performance tab** – Record page activity to find slow JavaScript or rendering
3. **Lighthouse tab** – Get a scored report with specific recommendations

### Automate with Playwright

```java
// Initialize Web Vitals tracking
initWebVitalsWithImport(page);

// Navigate to your page
page.navigate("https://yourapp.com");
page.waitForLoadState(LoadState.LOAD);

// Read metrics
Map<String, Object> vitals = page.evaluate("() => window.__vitals");

// Check results
log.info("Page load time: {}ms", vitals.get("LCP"));
log.info("Layout stability: {}", vitals.get("CLS"));
```

That's it. You now have real performance data.

---

## What You Need to Measure

### Initial Screen Load

This is the first page someone sees when opening your app. It shapes their first impression.

**Track:**
- Time until interactive
- Time until main content appears
- Total resources loaded
- Network transfer time

**Example:**
```
User opens app → Blank screen → Loading spinner → Content appears → User can interact
                 └─────────────────────────────────────────────────────┘
                              This is what we measure
```

### Navigation Between Pages

Most common user action. Should be fast and smooth.

**Track:**
- Click to page visible
- Resource loading time  
- DOM rendering time

**Example:**
```
Homepage → Click "External Cooperation" → Waiting → Page displays
          └──────────────────────────────────────────────────────┘
                       Navigation time
```

### Action Execution (Create/Edit/Save)

Direct impact on productivity. Slow saves frustrate users.

**Track:**
- Button click to server response
- Server processing time
- UI update time
- Total round-trip

**Example:**
```
Fill form → Click "Save" → Spinner → Success message
           └──────────────────────────────────────┘
                    Action time
```

---

## Code Breakdown

This section explains key JavaScript and Playwright APIs used in performance measurement.

### window
The global object in browser JavaScript. Everything runs in the context of `window`. We use it to store custom properties like `window.__vitals` for metrics that persist across page lifetime and can be accessed from Java tests.

### performance.clearResourceTimings()
Clears the browser's performance resource timing buffer. This prevents old data from previous page loads or actions from contaminating new measurements. Call this before starting a new performance test to ensure clean data.

### PerformanceObserver
A browser API that asynchronously observes performance events (like layout shifts, paints, or navigation). It doesn't block the main thread and can capture events that have already happened using `buffered: true`.

### .observe()
Method on `PerformanceObserver` that starts monitoring specific performance entry types (e.g., `'layout-shift'`, `'largest-contentful-paint'`). Takes an options object with `type` and `buffered` properties.

### .mark()
Method on `performance` object that creates a timestamp marker at the current time. Used to measure durations between two points (e.g., `performance.mark('start')` then `performance.mark('end')` then `performance.measure('duration', 'start', 'end')`).

### addInitScript()
Playwright method that injects JavaScript into every page/frame created during the session. Runs before any page content loads, perfect for setting up performance observers that need to capture events from the very beginning.

### getEntriesByType()
Method on `performance` object that returns an array of performance entries of a specific type (e.g., `'navigation'`, `'resource'`, `'paint'`). Each entry contains timing data like `startTime`, `loadEventEnd`, etc.

### [0] on getEntries()
Accesses the first element of the array returned by `getEntriesByType()`. Most entry types return a single entry (e.g., navigation has one entry per page), so `[0]` gets that entry. For types with multiple entries (like resources), you'd iterate or find specific ones.

---

## Automating with Playwright

### Understanding page.evaluate()

This is how you bridge Java and browser JavaScript.

**Concept:**
```
Java Test                    Browser
    │                           │
    │  page.evaluate(JS code)   │
    ├──────────────────────────>│
    │                           │ Runs JavaScript
    │                           │ Returns result
    │<──────────────────────────┤
    │                           │
```

**Example:**

```java
// Java sends JavaScript to browser
Object loadTime = page.evaluate("""
    () => {
        const nav = performance.getEntriesByType('navigation')[0];
        return nav.loadEventEnd - nav.startTime;
    }
""");

// Result comes back to Java
log.info("Page load: {}ms", loadTime);
// Output: Page load: 2341ms
```

The JavaScript runs in the browser and returns values to your Java test.

### Understanding window.__vitals

`window` is the global browser object. `__vitals` is a custom property we create to store metrics.

**Why we need it:**

We need somewhere to store metrics that:
- Persists during page lifetime
- Is accessible from any script
- Can be read from Java

**How it works:**

```javascript
// 1. Initialize empty object
window.__vitals = {};

// 2. Store metrics as they happen
window.__vitals.LCP = 1234;
window.__vitals.CLS = 0.05;
window.__vitals.INP = 89;

// 3. Java reads them later
Map<String, Object> vitals = page.evaluate("() => window.__vitals");
// Returns: {LCP: 1234, CLS: 0.05, INP: 89}
```

**Complete flow:**

```
addInitScript() → Page loads → Observers run → Java reads
       │              │            │              │
   Creates         Monitors     Stores in      Returns
   __vitals        metrics      __vitals       to Java
```

### Basic Performance Measurement

This method measures page load using the Performance API:

```java
private void executeDefaultPerformanceScripts(Page playwrightPage, String pageName) {
    // METRIC 1: Total page load time
    Object totalDuration = playwrightPage.evaluate("""
        () => {
          const navs = performance.getEntriesByType('navigation');
          if (!navs.length) return null;
          const nav = navs[0];
          return nav.loadEventEnd - nav.startTime;
        }
    """);

    // METRIC 2: Specific page render time
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

    // METRIC 3: Total resource download time
    Object totalDownloadDuration = playwrightPage.evaluate("""
        () => {
          const resources = performance.getEntriesByType('resource');
          if (!resources.length) return null;
          const firstStart = Math.min(...resources.map(r => r.startTime));
          const lastEnd = Math.max(...resources.map(r => r.responseEnd));
          return lastEnd - firstStart;
        }
    """);

    log.info("Server processing + network + DOM rendering: {}ms", totalDuration);
    log.info("Page render time for {}: {}ms", pageName, pageDuration);
    log.info("Total download duration: {}ms", totalDownloadDuration);
}
```

**What each metric shows:**

**Total Duration:**  
Navigation start → window.onload complete  
```
startTime                                     loadEventEnd
    ├──────────────────────────────────────────────┤
    │ DNS │ TCP │ Request │ Response │ DOM │ Load  │
```

**Page-Specific Duration:**  
Specific page start → last resource complete

**Total Download:**  
First resource start → last resource complete

### Real-World Examples

#### Measuring Initial Page Load

```java
@When("user navigate to External Cooperation Page through menu for performance validation")
public void validateExternalCooperationPagePerformance() {
    Page page = browserManager.getPage();

    // Navigate
    page.locator("//*[@id=\"mainmenu\"]/ul/li[3]/a").click();
    page.locator("//*[@id=\"mainmenu\"]/ul/li[3]/div/div/div[1]/a")
        .waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(10_000));
    page.locator("//*[@id=\"mainmenu\"]/ul/li[3]/div/div/div[1]/a").click();

    // Wait for page to fully load
    page.waitForLoadState(LoadState.NETWORKIDLE);
    assertOnPage(page, "//a[contains(@title,'home')]");
    page.waitForLoadState(LoadState.LOAD);

    // Measure
    executeDefaultPerformanceScripts(page, "cooperare-externa.nspx");
}
```

**Output:**
```
Server processing + network + DOM rendering: 2341ms
Page render time for cooperare-externa.nspx: 1987ms
Total download duration: 1654ms
```

#### Measuring Page Reload

```java
@When("validate page performance on reload")
public void pageReloadPerformanceValidation() {
    Page page = browserManager.getPage();

    // Clear previous performance data
    page.evaluate("performance.clearResourceTimings()");
    
    // Initialize Web Vitals tracking
    initWebVitalsWithImport(page);
    
    // Reload
    page.reload();
    page.waitForLoadState(LoadState.NETWORKIDLE);
    assertOnPage(page, "//a[contains(@title,'home')]");
    page.waitForLoadState(LoadState.LOAD);
    
    // Read metrics
    Map<String, Object> vitals = (Map<String, Object>) page.evaluate("() => window.__vitals");
    executeDefaultPerformanceScripts(page, "parlament.md");
    
    log.info("Web Vitals on Reload: {}", vitals);
}
```

**Output:**
```
Server processing + network + DOM rendering: 1987ms
Page render time for parlament.md: 1654ms
Total download duration: 1423ms
Web Vitals on Reload: {LCP=1456.0, CLS=0.05, INP=89.0, FCP=654.0, TTFB=234.0}
```

#### Measuring Action Execution (Search)

```java
@When("measure search duration")
public void measureSearchDuration() throws InterruptedException {
    Page page = browserManager.getPage();
    
    // Clear performance data
    page.evaluate("performance.clearResourceTimings()");
    
    // Perform search
    page.locator("//label/input[contains(@name,'q')]").fill("test");
    page.locator("//img[contains(@alt,'parliament of moldova search')]").click();
    
    // Mark start time
    page.evaluate("performance.mark('search-start')");
    
    // Wait for results
    assertOnPage(page, "//span[contains(@id,'search1')]//li/h2");
    page.waitForLoadState(LoadState.LOAD);
    page.waitForLoadState(LoadState.NETWORKIDLE);
    
    // Mark end time and measure
    page.evaluate("performance.mark('search-end')");
    page.evaluate("""
        performance.measure(
          'search-roundtrip',
          'search-start',
          'search-end'
        );
    """);
    
    Thread.sleep(1_000);
    
    // Calculate elapsed time (mark to last resource)
    Object elapsed = page.evaluate("""
        () => { 
            const markEntry = performance.getEntriesByName('search-start')[0]; 
            if (!markEntry) return -1; 
            const markTime = markEntry.startTime; 
            const navEntries = performance.getEntriesByType('navigation'); 
            const resEntries = performance.getEntriesByType('resource'); 
            const allEntries = [...navEntries, ...resEntries]; 
            const lastEndTime = Math.max(...allEntries.map(e => e.responseEnd)); 
            return lastEndTime - markTime;
        }
    """);
    
    // Get measurement duration (mark to mark)
    Object roundTrip = page.evaluate("""
        () => {
            const measure = performance.getEntriesByName('search-roundtrip')[0];
            return measure.duration;                
        }
    """);
    
    log.info("Search mark-to-mark duration: {}ms", roundTrip);
    log.info("Search complete round-trip: {}ms", elapsed);
}
```

**Output:**
```
Search mark-to-mark duration: 456ms
Search complete round-trip: 678ms
```

---

## Web Vitals – Core Metrics

Web Vitals are real user experience metrics defined by Google. They measure what users actually feel.

### CLS (Cumulative Layout Shift)

**What it measures:** Visual stability – how much content unexpectedly shifts during load

**Why it matters:** High CLS is frustrating and causes accidental clicks

**Example:**
```
User sees button → Moves to click → Ad loads above → Button shifts down → User clicks ad 
```

**Good threshold:** < 0.1

**The script:**

```javascript
let clsValue = 0;
let sessionValue = 0;
let sessionEntries = [];

new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    // Ignore shifts from user interaction
    if (entry.hadRecentInput) continue;

    const first = sessionEntries[0];
    const last = sessionEntries[sessionEntries.length - 1];

    // Session windowing (1s gap, 5s max)
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

    // Track maximum session value
    clsValue = Math.max(clsValue, sessionValue);
    storeMetric('CLS', clsValue);
  }
}).observe({ type: 'layout-shift', buffered: true });
```

**What it does:**

1. Monitors layout shifts in real-time
2. Ignores shifts caused by user actions (clicks, typing)
3. Groups shifts that happen close together (within 1s, max 5s window)
4. Takes the maximum session value as final CLS
5. Uses `buffered: true` to capture shifts that already happened

**Interpretation:**
- 0.0 - 0.1 = Good (stable layout)
- 0.1 - 0.25 = Needs improvement
- > 0.25 = Poor (lots of shifting)

**Common causes:**
- Images without width/height attributes
- Ads loading after content
- Fonts loading and reflowing text

### LCP (Largest Contentful Paint)

**What it measures:** Time until the largest visible element finishes loading

**Why it matters:** When the main content appears – "Is the page loaded?"

**Largest element is typically:**
- Hero image
- Large text block
- Video thumbnail

**Good threshold:** < 2.5 seconds

**The script:**

```javascript
new PerformanceObserver((list) => {
  const entries = list.getEntries();
  const lastEntry = entries[entries.length - 1];
  storeMetric('LCP', lastEntry.renderTime || lastEntry.loadTime);
}).observe({ type: 'largest-contentful-paint', buffered: true });
```

**What it does:**

1. Browser updates LCP as larger elements appear
2. Last entry is the final largest element
3. Uses renderTime (when painted) or loadTime (fallback)
4. `buffered: true` captures events that already happened

**Timeline example:**
```
Time 0.5s: Text block (LCP = 500ms)
Time 1.2s: Larger image (LCP updated to 1200ms)
Time 2.1s: Hero image (LCP updated to 2100ms) ← Final value
```

**Interpretation:**
- 0 - 2500ms = Good
- 2500 - 4000ms = Needs improvement
- > 4000ms = Poor

**Common causes:**
- Large uncompressed images (2MB hero image = 1.6s on 10 Mbps)
- Slow server response (TTFB > 500ms)
- Render-blocking CSS/JavaScript
- Client-side rendering

### FCP (First Contentful Paint)

**What it measures:** Time until ANY content first appears

**Why it matters:** Provides visual feedback that the page is loading

**Content includes:** Text, images, SVG, canvas

**Good threshold:** < 1.8 seconds

**The script:**

```javascript
new PerformanceObserver((list) => {
  const entry = list.getEntries()[0];
  storeMetric('FCP', entry.startTime);
}).observe({ type: 'paint', buffered: true });
```

**What it does:**

1. Observes paint events
2. First entry is FCP
3. Records timestamp when first content painted

**Interpretation:**
- 0 - 1800ms = Good
- 1800 - 3000ms = Needs improvement
- > 3000ms = Poor

**Relationship with LCP:**
```
0ms ────── FCP ──────────── LCP ──────────
       (First content)  (Main content)

FCP happens first (anything visible)
LCP happens later (main content visible)
```

### INP (Interaction to Next Paint)

**What it measures:** Responsiveness – time from user input to visual response

**Why it matters:** "Does the page respond to my clicks?"

**Measures:** Click response, keyboard input, tap response

**Good threshold:** < 200ms

**The script:**

```javascript
new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    storeMetric('INP', entry.duration);
  }
}).observe({ type: 'event', buffered: true, durationThreshold: 40 });
```

**What it does:**

1. Monitors user interactions
2. `durationThreshold: 40` filters out trivial interactions
3. Stores duration from input to paint
4. Typically, keeps the worst (longest) interaction

**Interpretation:**
- 0 - 200ms = Good
- 200 - 500ms = Needs improvement
- > 500ms = Poor

**Common causes:**
- Long JavaScript tasks blocking main thread
- Heavy event handlers
- Excessive DOM manipulation

### TTFB (Time To First Byte)

**What it measures:** Server performance – time from request to first byte received

**Why it matters:** Measures server speed and network latency

**Includes:** DNS lookup, TCP connection, server processing, network latency

**Good threshold:** < 500ms

**The script:**

```javascript
new PerformanceObserver((list) => {
  const entry = list.getEntries()[0];
  storeMetric('TTFB', entry.responseStart);
}).observe({ type: 'navigation', buffered: true });
```

**What it does:**

1. Gets navigation timing entry
2. `responseStart` is when first byte received
3. This is TTFB

**Breakdown:**
```
TTFB = DNS + TCP + Server Processing

Example:
DNS lookup:       20ms
TCP connection:   30ms
Server processing: 184ms
────────────────────────
TTFB:             234ms 
```

**Interpretation:**
- 0 - 500ms = Good
- 500 - 1000ms = Needs improvement
- > 1000ms = Poor

**Common causes:**
- Slow database queries
- Geographic distance to server
- No caching

### Complete Web Vitals Implementation

```java
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
        
          // INP
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
```

**Usage:**

```java
@Test
public void measureWebVitals() {
    Page page = browserManager.getPage();
    
    // Initialize tracking BEFORE navigation
    initWebVitals(page);
    
    // Navigate
    page.navigate("https://example.com");
    page.waitForLoadState(LoadState.LOAD);
    
    // Read metrics
    Map<String, Object> vitals = page.evaluate("() => window.__vitals");
    
    // Log results
    log.info("Web Vitals Report:");
    log.info("CLS:  {} (Good: <0.1)", vitals.get("CLS"));
    log.info("LCP:  {}ms (Good: <2500ms)", vitals.get("LCP"));
    log.info("FCP:  {}ms (Good: <1800ms)", vitals.get("FCP"));
    log.info("INP:  {}ms (Good: <200ms)", vitals.get("INP"));
    log.info("TTFB: {}ms (Good: <500ms)", vitals.get("TTFB"));
    
    // Assert thresholds
    assertTrue((Double) vitals.get("CLS") < 0.1, "CLS should be <0.1");
    assertTrue((Double) vitals.get("LCP") < 2500, "LCP should be <2.5s");
    assertTrue((Double) vitals.get("TTFB") < 500, "TTFB should be <500ms");
}
```

**Output:**
```
Web Vitals Report:

CLS:  0.05 (Good: <0.1) 
LCP:  1456ms (Good: <2500ms)
FCP:  654ms (Good: <1800ms) 
INP:  89ms (Good: <200ms) 
TTFB: 234ms (Good: <500ms) 
```

---

## Simplified Approach: Using the Web Vitals Library

### Why Use a Library?

**Problem with custom implementation:**
- Complex logic (CLS session windowing is tricky)
- Need to keep up with spec changes
- More code to maintain

**Solution: web-vitals library from Google**
- Official implementation
- Handles all complexity
- Always up-to-date
- Much simpler code

### Implementation

```java
public void initWebVitalsWithImport(Page playwrightPage) {
    playwrightPage.addInitScript("""
        // Load web-vitals library from CDN
        const script = document.createElement('script');
        script.src = 'https://unpkg.com/web-vitals@3/dist/web-vitals.iife.js';
        
        // Set up metric collection when library loads
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
```

**What it does:**

1. Creates a script element
2. Loads web-vitals library from CDN (~10KB)
3. When loaded, registers callbacks for each metric
4. Stores metric values in `window.__vitals`

**How it works:**

```
addInitScript()          Page loads        Read metrics
      │                      │                  │
  Load library          Measure vitals     Returns to Java
  Set up callbacks      Store in __vitals
```

### Comparison

| Aspect | Custom (initWebVitals) | Library (initWebVitalsWithImport) |
|--------|----------------------|----------------------------------|
| Code Length | ~100 lines | ~15 lines |
| Complexity | High | Low |
| Accuracy | Good (if correct) | Excellent (official) |
| Maintenance | You maintain | Google maintains |
| Performance | Slightly faster | +10KB download |

### Usage Example

```java
@Test
public void measureWithLibrary() {
    Page page = browserManager.getPage();
    
    // Clear previous data
    page.evaluate("performance.clearResourceTimings()");
    
    // Initialize (simplified!)
    initWebVitalsWithImport(page);
    
    // Navigate
    page.navigate("https://example.com");
    page.waitForLoadState(LoadState.LOAD);
    page.waitForLoadState(LoadState.NETWORKIDLE);
    
    // Read metrics (same as before!)
    Map<String, Object> vitals = page.evaluate("() => window.__vitals");
    
    log.info("Web Vitals: {}", vitals);
    // Output: {LCP=1456.0, CLS=0.05, INP=89.0, FCP=654.0, TTFB=234.0}
}
```

---

## References

### Official Documentation

- [Web Performance API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Performance)
- [Playwright Java Documentation](https://playwright.dev/java/)
- [Chrome DevTools Performance](https://developer.chrome.com/docs/devtools/performance/)
- [Web Vitals](https://web.dev/articles/vitals)
- [Largest Contentful Paint (LCP)](https://web.dev/articles/lcp)
- [Interaction to Next Paint (INP)](https://web.dev/articles/inp)
- [Cumulative Layout Shift (CLS)](https://web.dev/articles/cls)
- [First Contentful Paint (FCP)](https://web.dev/articles/fcp)
- [Time to First Byte (TTFB)](https://web.dev/articles/ttfb)
- [Custom Metrics](https://web.dev/articles/custom-metrics)

### Tools

- [Chrome DevTools](https://developer.chrome.com/docs/devtools/)
- [Lighthouse](https://developer.chrome.com/docs/lighthouse/)
- [Web Vitals Library](https://github.com/GoogleChrome/web-vitals)
- [WebPageTest](https://webpagetest.org/)

### Specifications

- [Navigation Timing Level 2](https://w3c.github.io/navigation-timing/)
- [Resource Timing Level 2](https://w3c.github.io/resource-timing/)
- [Performance Timeline Level 2](https://w3c.github.io/performance-timeline/)
- [Layout Instability API](https://wicg.github.io/layout-instability/)

---

