package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
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
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // 1. Login
            log.info("Logging into portal: {}", config.getLoginUrl());
            page.navigate(config.getLoginUrl());
            page.fill(config.getUsernameSelector(), username);
            page.fill(config.getPasswordSelector(), password);
            page.click(config.getSubmitSelector());
            page.waitForLoadState();

            // 2. Navigate to Portal Action URL if different
            if (config.getPortalUrl() != null && !config.getPortalUrl().isEmpty() && !config.getPortalUrl().equals(config.getLoginUrl())) {
                log.info("Navigating to action URL: {}", config.getPortalUrl());
                page.navigate(config.getPortalUrl());
            }

            // 3. Fill Form using Extracted Data
            log.info("Filling form with extracted data for action: {}", config.getActionName());
            if (config.getFormSelectors() != null) {
                for (Map.Entry<String, String> entry : config.getFormSelectors().entrySet()) {
                    String fieldName = entry.getKey();
                    String selector = entry.getValue();
                    Object value = data.get(fieldName);
                    if (value != null) {
                        page.fill(selector, value.toString());
                    }
                }
            }

            // 4. File Upload (if applicable)
            if (config.getFileInputSelector() != null && filePath != null) {
                log.info("Uploading file to selector: {}", config.getFileInputSelector());
                page.setInputFiles(config.getFileInputSelector(), filePath);
            }

            // 5. Submit & Capture Result
            log.info("Submitting form for action: {}", config.getActionName());
            page.click("button[type='submit'], .submit-btn, #submit-claim"); 
            page.waitForLoadState();

            String resultId = "UNKNOWN";
            if (config.getResultSelector() != null) {
                try {
                    page.waitForSelector(config.getResultSelector(), new Page.WaitForSelectorOptions().setTimeout(10000));
                    resultId = page.innerText(config.getResultSelector()).trim();
                } catch (Exception e) {
                    log.warn("Could not capture result ID using selector {}: {}", config.getResultSelector(), e.getMessage());
                }
            }

            log.info("Action {} completed. Captured ID: {}", config.getActionName(), resultId);
            browser.close();
            return resultId;
        } catch (Exception e) {
            log.error("Portal Bridge execution failed for action: {}", config.getActionName(), e);
            throw new RuntimeException("Automation failed: " + e.getMessage());
        }
    }
}
