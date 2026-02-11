package com.ailytics.ailytics.service;

import com.ailytics.ailytics.dto.AutomationRequest;
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutomationService {

    private final GeminiService geminiService;
    private final Map<String, String> jobStatus = new ConcurrentHashMap<>();

    public String triggerAutomation(AutomationRequest request) {
        String jobId = java.util.UUID.randomUUID().toString();
        jobStatus.put(jobId, "IN_PROGRESS");

        CompletableFuture.runAsync(() -> {
            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = browser.newPage();
                
                log.info("Starting automation for URL: {}", request.getUrl());
                page.navigate(request.getUrl());

                // Semantic Selection Logic (Simplified)
                // In a real scenario, we'd pass the HTML/DOM to GeminiService to find selectors
                // For this demo, we use common selectors or AI-guided discovery
                
                String loginPrompt = "Given this page content, identify the CSS selectors for the username field, password field, and login button. Return JSON: {username: '', password: '', submit: ''}";
                // String selectorsJson = geminiService.processDocument(null, null, loginPrompt); 
                
                // For now, using standard selectors as placeholders for the orchestrator logic
                page.fill("input[name='username'], input[type='text'], #username", request.getUsername());
                page.fill("input[name='password'], input[type='password'], #password", request.getPassword());
                page.click("button[type='submit'], #login-button, .login-btn");

                page.waitForLoadState();

                if (request.getFormData() != null) {
                    for (Map.Entry<String, String> entry : request.getFormData().entrySet()) {
                        page.fill("input[name='" + entry.getKey() + "'], #" + entry.getKey(), entry.getValue());
                    }
                }

                jobStatus.put(jobId, "COMPLETED");
                log.info("Automation completed for jobId: {}", jobId);
                browser.close();
            } catch (Exception e) {
                log.error("Automation failed for jobId: {}", jobId, e);
                jobStatus.put(jobId, "FAILED: " + e.getMessage());
            }
        });

        return jobId;
    }

    public String getStatus(String jobId) {
        return jobStatus.getOrDefault(jobId, "NOT_FOUND");
    }
}
