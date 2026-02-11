package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortalBridgeService {

    private final GeminiService geminiService;
    private final MetadataService metadataService;

    @Value("${playwright.headless:true}")
    private boolean headless;

    public String executeAutomation(String jobId, ActionConfig config, Map<String, Object> data, String username, String password, Path filePath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            try {
                // 1. Semantic Login Logic
                log.info("Performing semantic login for action: {}", config.getActionName());
                page.navigate(config.getLoginUrl());
                
                // Try to find login fields semantically if selectors aren't provided
                fillSemantically(page, "Username", username);
                fillSemantically(page, "Password", password);
                clickSemantically(page, "Login", "Sign In", "Submit");
                
                page.waitForLoadState();

                String result = executeStepsWithHealing(page, config, data, filePath);
                browser.close();
                return result;
            } catch (Exception e) {
                log.error("Semantic Automation Flow failed", e);
                browser.close();
                throw new RuntimeException("Automation Pipeline Error: " + e.getMessage());
            }
        }
    }

    private String executeStepsWithHealing(Page page, ActionConfig config, Map<String, Object> data, Path filePath) {
        String resultId = "COMPLETED";
        boolean recipeChanged = false;

        if (config.getSteps() != null) {
            for (AutomationStep step : config.getSteps()) {
                log.info("Executing step: {}", step.getType());
                try {
                    resultId = executeStep(page, step, data, filePath);
                } catch (Exception e) {
                    log.warn("Step failed, attempting self-healing for step type: {}", step.getType());
                    if (attemptHealing(page, step, config)) {
                        recipeChanged = true;
                        // Retry the step with new selector
                        resultId = executeStep(page, step, data, filePath);
                    } else {
                        throw e; // Healing failed
                    }
                }
            }
        }

        if (recipeChanged) {
            log.info("Saving evolved recipe (self-healed) for action: {}", config.getActionName());
            metadataService.saveConfig(config);
        }

        return resultId;
    }

    private String executeStep(Page page, AutomationStep step, Map<String, Object> data, Path filePath) {
        String resultId = "COMPLETED";
        switch (step.getType()) {
            case NAVIGATE -> page.navigate(step.getTargetUrl());
            case FILL_FORM -> {
                if (step.getFields() != null) {
                    for (String fieldKey : step.getFields()) {
                        Object value = getNestedValue(data, fieldKey);
                        if (value != null) {
                            String labelHint = splitCamelCase(fieldKey);
                            fillSemantically(page, labelHint, value.toString());
                        }
                    }
                }
            }
            case CLICK -> {
                if (step.getSelector() != null && !step.getSelector().isEmpty()) {
                    try {
                        page.click(step.getSelector(), new Page.ClickOptions().setTimeout(5000));
                    } catch (Exception e) {
                        // If standard selector fails, try semantic click
                        clickSemantically(page, step.getSelector());
                    }
                }
            }
            case UPLOAD_FILE -> {
                if (filePath != null) {
                    String selector = (step.getSelector() != null && !step.getSelector().isEmpty()) ? step.getSelector() : "input[type='file']";
                    page.setInputFiles(selector, filePath);
                }
            }
            case WAIT_FOR_LOAD -> page.waitForLoadState();
            case CAPTURE_RESULT -> {
                resultId = page.innerText(step.getSelector()).trim();
            }
        }
        return resultId;
    }

    private boolean attemptHealing(Page page, AutomationStep step, ActionConfig config) {
        log.info("Self-healing triggered. Capturing DOM...");
        String dom = page.content();
        
        String description = switch (step.getType()) {
            case CLICK -> "The button or link with label/selector: " + step.getSelector();
            case FILL_FORM -> "The input fields for: " + String.join(", ", step.getFields());
            case CAPTURE_RESULT -> "The element containing the final result ID (previous selector: " + step.getSelector() + ")";
            case UPLOAD_FILE -> "The file upload input (previous selector: " + step.getSelector() + ")";
            default -> step.getType().toString();
        };

        try {
            String newSelector = geminiService.healSelector(dom, description);
            if (newSelector != null && !newSelector.isEmpty() && !newSelector.contains("not found")) {
                log.info("Gemini suggested new selector: {}", newSelector);
                step.setSelector(newSelector);
                return true;
            }
        } catch (Exception e) {
            log.error("Gemini failed to heal selector", e);
        }
        return false;
    }

    private void fillSemantically(Page page, String label, String value) {
        log.info("Attempting to fill field semantically: '{}'", label);
        try {
            Locator locator = page.getByLabel(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
            if (locator.count() == 0) {
                locator = page.getByPlaceholder(Pattern.compile(label, Pattern.CASE_INSENSITIVE));
            }
            if (locator.count() == 0) {
                locator = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(Pattern.compile(label, Pattern.CASE_INSENSITIVE)));
            }

            if (locator.count() > 0) {
                locator.first().fill(value);
            } else {
                // If semantic finding fails, throw exception to trigger healing if applicable
                throw new RuntimeException("Could not find field: " + label);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error filling field '" + label + "': " + e.getMessage());
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
                log.debug("Click attempt failed for label '{}'", label);
            }
        }
        throw new RuntimeException("Could not find button semantically: " + String.join(", ", labels));
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
