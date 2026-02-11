package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.model.WorkflowResult;
import com.ailytics.ailytics.repository.WorkflowResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final MetadataService metadataService;
    private final GeminiService geminiService;
    private final PortalBridgeService portalBridgeService;
    private final WorkflowResultRepository workflowResultRepository;

    public String startWorkflow(String actionName, Resource file, String contentType, String username, String password) {
        String workflowId = java.util.UUID.randomUUID().toString();
        
        WorkflowResult initialResult = WorkflowResult.builder()
                .workflowId(workflowId)
                .actionName(actionName)
                .status("EXTRACTING")
                .build();
        workflowResultRepository.save(initialResult);

        CompletableFuture.runAsync(() -> {
            Path tempFile = null;
            try {
                ActionConfig config = metadataService.getConfig(actionName);
                if (config == null) throw new RuntimeException("Action not found: " + actionName);

                // Save file temporarily for Playwright
                tempFile = Files.createTempFile("ailytics-", ".tmp");
                try (InputStream is = file.getInputStream()) {
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Phase 1: Extraction
                log.info("Starting extraction for workflow: {}", workflowId);
                Map<String, Object> extractedData = geminiService.extractData(file, contentType, config.getExtractionSchema());
                log.info("Extracted data for {}: {}", workflowId, extractedData);

                // Phase 2: Automation
                updateStatus(workflowId, "AUTOMATING", null);
                String resultId = portalBridgeService.executeAutomation(config, extractedData, username, password, tempFile);

                updateStatus(workflowId, "COMPLETED", resultId);
            } catch (Exception e) {
                log.error("Workflow failed: {}", workflowId, e);
                updateStatus(workflowId, "FAILED: " + e.getMessage(), null);
            } finally {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (Exception e) {
                        log.warn("Could not delete temp file {}: {}", tempFile, e.getMessage());
                    }
                }
            }
        });

        return workflowId;
    }

    private void updateStatus(String workflowId, String status, String resultId) {
        workflowResultRepository.findById(workflowId).ifPresent(res -> {
            res.setStatus(status);
            if (resultId != null) res.setResultId(resultId);
            workflowResultRepository.save(res);
        });
    }

    public WorkflowResult getStatus(String workflowId) {
        return workflowResultRepository.findById(workflowId).orElse(null);
    }
}
