package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.model.WorkflowResult;
import com.ailytics.ailytics.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("action") String action,
            @RequestParam("username") String username,
            @RequestParam("password") String password) {

        try {
            String workflowId = workflowService.startWorkflow(
                    action, 
                    file.getResource(), 
                    file.getContentType(), 
                    username, 
                    password
            );

            return ResponseEntity.accepted().body(Map.of(
                    "workflowId", workflowId,
                    "status", "STARTED",
                    "message", "Document received. Processing extraction and automation."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{workflowId}")
    public ResponseEntity<WorkflowResult> getStatus(@PathVariable String workflowId) {
        WorkflowResult result = workflowService.getStatus(workflowId);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }
}
