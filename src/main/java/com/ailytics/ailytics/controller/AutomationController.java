package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.dto.AutomationRequest;
import com.ailytics.ailytics.service.AutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeAutomation(@RequestBody AutomationRequest request) {
        String jobId = automationService.triggerAutomation(request);
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "IN_PROGRESS",
            "message", "Automation task started asynchronously."
        ));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String jobId) {
        String status = automationService.getStatus(jobId);
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "status", status
        ));
    }
}
