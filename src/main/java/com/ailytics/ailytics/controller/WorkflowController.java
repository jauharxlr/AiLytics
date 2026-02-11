package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.model.ProcessingQueue;
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
            String jobId = workflowService.enqueueWorkflow(file, action, username, password);

            return ResponseEntity.accepted().body(Map.of(
                    "jobId", jobId,
                    "status", "QUEUED",
                    "message", "Document received and enqueued for processing."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ProcessingQueue> getStatus(@PathVariable String jobId) {
        ProcessingQueue job = workflowService.getJobStatus(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }
}
