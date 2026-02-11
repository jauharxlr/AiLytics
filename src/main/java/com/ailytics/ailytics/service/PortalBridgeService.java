package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
@Slf4j
public class PortalBridgeService {

    @Value("${playwright.headless:true}")
    private boolean headless;

    public String executeAutomation(ActionConfig config, Map<String, Object> data, String username, String password, Path filePath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            Page page = browser.newPage();

            // 1. Mandatory Login Step
            log.info("Logging into portal for action: {}", config.getActionName());
            page.navigate(config.getLoginUrl());
            page.fill(config.getUsernameSelector(), username);
            page.fill(config.getPasswordSelector(), password);
            page.click(config.getLoginSubmitSelector());
            page.waitForLoadState();

            String resultId = "COMPLETED";

            // 2. Execute Dynamic Steps (Wizard Flow)
            if (config.getSteps() != null) {
                for (AutomationStep step : config.getSteps()) {
                    log.info("Executing step: {}", step.getType());
                    switch (step.getType()) {
                        case NAVIGATE -> page.navigate(step.getTargetUrl());
                        case FILL_FORM -> {
                            for (Map.Entry<String, String> entry : step.getFieldMapping().entrySet()) {
                                Object value = data.get(entry.getKey());
                                if (value != null) {
                                    page.fill(entry.getSelector(), value.toString());
                                }
                            }
                        }
                        case CLICK -> page.click(step.getSelector());
                        case UPLOAD_FILE -> {
                            if (filePath != null) {
                                page.setInputFiles(step.getSelector(), filePath);
                            }
                        }
                        case WAIT_FOR_LOAD -> page.waitForLoadState();
                        case CAPTURE_RESULT -> {
                            try {
                                resultId = page.innerText(step.getSelector()).trim();
                            } catch (Exception e) {
                                log.warn("Capture result failed: {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            browser.close();
            return resultId;
        } catch (Exception e) {
            log.error("Generic Automation Flow failed", e);
            throw new RuntimeException("Automation Pipeline Error: " + e.getMessage());
        }
    }
}
