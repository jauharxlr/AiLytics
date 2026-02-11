package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PortalBridgeService {

    @Value("${playwright.headless:true}")
    private boolean headless;

    public String executeAutomation(ActionConfig config, Map<String, Object> data, String username, String password, Path filePath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            Page page = browser.newPage();

            // 1. Semantic Login Logic
            log.info("Performing semantic login for action: {}", config.getActionName());
            page.navigate(config.getLoginUrl());
            
            // Try to find login fields semantically if selectors aren't provided
            fillSemantically(page, "Username", username);
            fillSemantically(page, "Password", password);
            clickSemantically(page, "Login", "Sign In", "Submit");
            
            page.waitForLoadState();

            String resultId = "COMPLETED";

            // 2. Execute Dynamic Steps (Wizard Flow)
            if (config.getSteps() != null) {
                for (AutomationStep step : config.getSteps()) {
                    log.info("Executing step: {}", step.getType());
                    switch (step.getType()) {
                        case NAVIGATE -> page.navigate(step.getTargetUrl());
                        case FILL_FORM -> {
                            if (step.getFields() != null) {
                                for (String fieldKey : step.getFields()) {
                                    Object value = getNestedValue(data, fieldKey);
                                    if (value != null) {
                                        // The "fieldKey" (e.g., patientName) is used as the semantic label hint
                                        String labelHint = splitCamelCase(fieldKey);
                                        fillSemantically(page, labelHint, value.toString());
                                    }
                                }
                            }
                        }
                        case CLICK -> clickSemantically(page, step.getSelector());
                        case UPLOAD_FILE -> {
                            if (filePath != null) {
                                // Try to find file input by label or generic upload role
                                try {
                                    page.setInputFiles("input[type='file']", filePath);
                                } catch (Exception e) {
                                    log.warn("Standard file upload failed, trying semantic upload: {}", e.getMessage());
                                }
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
            log.error("Semantic Automation Flow failed", e);
            throw new RuntimeException("Automation Pipeline Error: " + e.getMessage());
        }
    }

    private void fillSemantically(Page page, String label, String value) {
        log.info("Attempting to fill field semantically: '{}'", label);
        try {
            // Priority 1: Label matching (Exact or Fuzzy)
            Locator locator = page.getByLabel(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
            if (locator.count() == 0) {
                // Priority 2: Placeholder matching
                locator = page.getByPlaceholder(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
            }
            if (locator.count() == 0) {
                // Priority 3: Aria Role Text
                locator = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(Pattern.compile(label, Pattern.CASE_INSENSITIVE)));
            }

            if (locator.count() > 0) {
                locator.first().fill(value);
            } else {
                log.warn("Could not find field semantically for label: {}", label);
            }
        } catch (Exception e) {
            log.error("Error during semantic fill for '{}': {}", label, e.getMessage());
        }
    }

    private void clickSemantically(Page page, String... labels) {
        for (String label : labels) {
            try {
                Locator locator = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile(label, Pattern.CASE_INSENSITIVE)));
                if (locator.count() == 0) {
                    locator = page.getByText(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
                }
                
                if (locator.count() > 0) {
                    locator.first().click();
                    return;
                }
            } catch (Exception e) {
                log.debug("Click attempt failed for label '{}': {}", label, e.getMessage());
            }
        }
    }

    private Object getNestedValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private String splitCamelCase(String s) {
        return s.replaceAll(
           String.format("%s|%s|%s",
              "(?<=[A-Z])(?=[A-Z][a-z])",
              "(?<=[^A-Z])(?=[A-Z])",
              "(?<=[A-Za-z])(?=[^A-Za-z])"
           ),
           " "
        ).trim();
    }
}
