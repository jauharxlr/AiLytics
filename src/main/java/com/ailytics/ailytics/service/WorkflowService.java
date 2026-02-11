package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final MetadataService metadataService;
    private final GeminiService geminiService;
    private final PortalBridgeService portalBridgeService;

    private final Map<String, String> workflowStatus = new ConcurrentHashMap<>();

    public String startWorkflow(String actionName, Resource file, String contentType, String username, String password) {
        String workflowId = java.util.UUID.randomUUID().toString();
        workflowStatus.put(workflowId, "EXTRACTING");

        CompletableFuture.runAsync(() -> {
            try {
                ActionConfig config = metadataService.getConfig(actionName);
                if (config == null) throw new RuntimeException("Action not found: " + actionName);

                // Phase 1: Extraction
                log.info("Starting extraction for workflow: {}", workflowId);
                Map<String, Object> extractedData = geminiService.extractData(file, contentType, config.getExtractionSchema());
                log.info("Extracted data: {}", extractedData);

                // Phase 2: Automation
                workflowStatus.put(workflowId, "AUTOMATING");
                String resultId = portalBridgeService.executeAutomation(config, extractedData, username, password);

                workflowStatus.put(workflowId, "COMPLETED: " + resultId);
            } catch (Exception e) {
                log.error("Workflow failed: {}", workflowId, e);
                workflowStatus.put(workflowId, "FAILED: " + e.getMessage());
            }
        });

        return workflowId;
    }

    public String getStatus(String workflowId) {
        return workflowStatus.getOrDefault(workflowId, "NOT_FOUND");
    }
}
