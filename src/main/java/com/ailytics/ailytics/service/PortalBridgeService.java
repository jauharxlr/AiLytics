package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.AutomationStep;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${playwright.debug-mode:false}")
    private boolean debugMode;

    private static final String DEBUG_BASE_PATH = "debug";

    public String executeAutomation(String jobId, ActionConfig config, Map<String, Object> data, Map<String, Object> contextData, String username, String password, Path filePath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
            if (debugMode) {
                Path videoPath = Paths.get(DEBUG_BASE_PATH, "videos", jobId);
                ensureDirectory(videoPath);
                contextOptions.setRecordVideoDir(videoPath);
                contextOptions.setRecordVideoSize(1280, 720);
            }

            BrowserContext context = browser.newContext(contextOptions);
            Page page = context.newPage();

            try {
                // 1. Semantic Login Logic
                log.info("Performing semantic login for action: {}", config.getActionName());
                page.navigate(config.getLoginUrl());
                
                fillSemantically(page, "Username", username);
                takeStepScreenshot(page, jobId, "login_user_filled");
                
                fillSemantically(page, "Password", password);
                takeStepScreenshot(page, jobId, "login_pass_filled");
                
                clickSemantically(page, "Login", "Sign In", "Submit");
                page.waitForLoadState();
                takeStepScreenshot(page, jobId, "after_login");

                String result = executeStepsWithHealing(page, jobId, config, data, contextData, filePath);
                
                context.close(); // Important to finalize video recording
                browser.close();
                return result;
            } catch (Exception e) {
                log.error("Semantic Automation Flow failed", e);
                takeStepScreenshot(page, jobId, "error_final");
                context.close();
                browser.close();
                throw new RuntimeException("Automation Pipeline Error: " + e.getMessage());
            }
        }
    }

    private String executeStepsWithHealing(Page page, String jobId, ActionConfig config, Map<String, Object> data, Map<String, Object> contextData, Path filePath) {
        String resultId = "COMPLETED";
        boolean recipeChanged = false;
        int stepIndex = 1;

        if (config.getSteps() != null) {
            for (AutomationStep step : config.getSteps()) {
                log.info("Executing step: {}", step.getType());
                try {
                    resultId = executeStep(page, jobId, stepIndex, step, data, contextData, filePath);
                    stepIndex++;
                } catch (Exception e) {
                    log.warn("Step failed, attempting self-healing for step type: {}", step.getType());
                    if (attemptHealing(page, step, config)) {
                        recipeChanged = true;
                        resultId = executeStep(page, jobId, stepIndex, step, data, contextData, filePath);
                        takeStepScreenshot(page, jobId, "step_" + stepIndex + "_healed");
                        stepIndex++;
                    } else {
                        throw e;
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

    private String executeStep(Page page, String jobId, int index, AutomationStep step, Map<String, Object> data, Map<String, Object> contextData, Path filePath) {
        String resultId = "COMPLETED";
        switch (step.getType()) {
            case NAVIGATE -> page.navigate(step.getTargetUrl());
            case FILL_FORM -> {
                if (step.getFields() != null) {
                    for (String fieldKey : step.getFields()) {
                        Object value = resolveValue(fieldKey, data, contextData);
                        if (value != null) {
                            String labelHint = splitCamelCase(fieldKey.replace("{{context.", "").replace("}}", ""));
                            fillSemantically(page, labelHint, value.toString());
                            takeStepScreenshot(page, jobId, "step_" + index + "_fill_" + labelHint);
                        }
                    }
                }
            }
            case CLICK -> {
                if (step.getSelector() != null && !step.getSelector().isEmpty()) {
                    try {
                        page.click(step.getSelector(), new Page.ClickOptions().setTimeout(5000));
                    } catch (Exception e) {
                        clickSemantically(page, step.getSelector());
                    }
                    takeStepScreenshot(page, jobId, "step_" + index + "_click");
                }
            }
            case UPLOAD_FILE -> {
                if (filePath != null) {
                    String selector = (step.getSelector() != null && !step.getSelector().isEmpty()) ? step.getSelector() : "input[type='file']";
                    page.setInputFiles(selector, filePath);
                    takeStepScreenshot(page, jobId, "step_" + index + "_upload");
                }
            }
            case WAIT_FOR_LOAD -> {
                if (step.getWaitSelector() != null && !step.getWaitSelector().isEmpty()) {
                    page.waitForSelector(step.getWaitSelector());
                } else {
                    page.waitForLoadState();
                }
            }
            case CAPTURE_RESULT -> {
                resultId = page.innerText(step.getSelector()).trim();
            }
            case CAPTURE_TO_CONTEXT -> {
                String val = page.innerText(step.getSelector()).trim();
                log.info("Captured value '{}' to context key '{}'", val, step.getContextKey());
                contextData.put(step.getContextKey(), val);
            }
            case NEXT_RECIPE -> {
                return "TRIGGER_CHAIN:" + step.getNextRecipeName();
            }
        }
        return resultId;
    }

    private Object resolveValue(String key, Map<String, Object> data, Map<String, Object> contextData) {
        if (key.startsWith("{{context.") && key.endsWith("}}")) {
            String contextKey = key.substring(10, key.length() - 2);
            return contextData.get(contextKey);
        }
        return getNestedValue(data, key);
    }

    private void takeStepScreenshot(Page page, String jobId, String actionName) {
        if (!debugMode) return;
        try {
            Path path = Paths.get(DEBUG_BASE_PATH, "screenshots", jobId, actionName + ".png");
            ensureDirectory(path.getParent());
            page.screenshot(new Page.ScreenshotOptions().setPath(path));
            log.info("Debug screenshot saved: {}", path);
        } catch (Exception e) {
            log.warn("Failed to take debug screenshot: {}", e.getMessage());
        }
    }

    private void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Could not create debug directory: {}", path);
        }
    }

    private boolean attemptHealing(Page page, AutomationStep step, ActionConfig config) {
        log.info("Self-healing triggered. Capturing DOM...");
        String dom = page.content();
        
        String description = switch (step.getType()) {
            case CLICK -> "The button or link with label/selector: " + step.getSelector();
            case FILL_FORM -> "The input fields for: " + String.join(", ", step.getFields());
            case CAPTURE_RESULT, CAPTURE_TO_CONTEXT -> "The element containing the text/ID (previous selector: " + step.getSelector() + ")";
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
