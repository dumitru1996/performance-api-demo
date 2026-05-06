package org.example.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.example.utils.LogUtils;

import java.util.Random;

@Slf4j
public class GlobalHooks {

    /**
     * Executed before every scenario.
     * Creates a unique log directory name derived from the scenario ID and sets it in MDC.
     */
    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {
        String scenarioDir = sanitiseDirName(scenario.getName());
        LogUtils.putMDCValue(LogUtils.MDC_SCENARIO_DIR, scenarioDir);
        log.info("========== START: [{}] ==========", scenario.getName());
    }

    /**
     * Executed after every scenario.
     * Logs the final status, clears MDC.
     */
    @After(order = 0)
    public void afterScenario(Scenario scenario) {
        log.info("========== END [{}]: {} ==========", scenario.getStatus(), scenario.getName());
        LogUtils.clearMDCValue();
    }

    private String sanitiseDirName(String scenarioName) {
        return scenarioName
                .replaceAll("[^a-zA-Z0-9._\\-]", "_")
                .replaceAll("_{2,}", "_");
    }
}

