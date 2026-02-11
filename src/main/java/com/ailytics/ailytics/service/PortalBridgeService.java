package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PortalBridgeService {

    public String executeAutomation(ActionConfig config, Map<String, Object> data, String username, String password) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // 1. Login
            log.info("Logging into portal: {}", config.getLoginUrl());
            page.navigate(config.getLoginUrl());
            page.fill(config.getUsernameSelector(), username);
            page.fill(config.getPasswordSelector(), password);
            page.click(config.getSubmitSelector());
            page.waitForLoadState();

            // 2. Navigate to Portal Action URL if different
            if (!config.getPortalUrl().equals(config.getLoginUrl())) {
                page.navigate(config.getPortalUrl());
            }

            // 3. Fill Form using Extracted Data
            log.info("Filling form with extracted data for action: {}", config.getActionName());
            for (Map.Entry<String, String> entry : config.getFormSelectors().entrySet()) {
                String fieldName = entry.getKey();
                String selector = entry.getValue();
                Object value = data.get(fieldName);
                if (value != null) {
                    page.fill(selector, value.toString());
                }
            }

            // 4. Submit & Capture Result
            // Assuming there's a submit button on the form page
            page.click("button[type='submit'], .submit-btn"); 
            page.waitForLoadState();

            String resultId = "UNKNOWN";
            try {
                resultId = page.innerText(config.getResultSelector());
            } catch (Exception e) {
                log.warn("Could not capture result ID: {}", e.getMessage());
            }

            log.info("Action {} completed. Captured ID: {}", config.getActionName(), resultId);
            browser.close();
            return resultId;
        } catch (Exception e) {
            log.error("Portal Bridge execution failed", e);
            throw new RuntimeException("Automation failed: " + e.getMessage());
        }
    }
}
