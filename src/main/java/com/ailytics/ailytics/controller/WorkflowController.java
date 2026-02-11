package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(
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
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String workflowId) {
        String status = workflowService.getStatus(workflowId);
        return ResponseEntity.ok(Map.of(
                "workflowId", workflowId,
                "status", status
        ));
    }
}
