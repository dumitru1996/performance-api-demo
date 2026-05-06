package org.example.utils;

import org.slf4j.MDC;

public class LogUtils {

    public static final String TARGET_FILE = "target";
    public static final String LOGS_FILE = "logs";
    public static final String SCREENSHOTS_FILE = "screenshots";
    public static final String MDC_SCENARIO_DIR = "scenarioDir";
    public static final String MDC_SCREENSHOTS_DIR = "screenshotsDir";

    private LogUtils() {
    }

    public static void putMDCValue(String key, String value) {
        MDC.put(key, value);
    }

    public static void clearMDCValue() {
        MDC.clear();
    }
}

