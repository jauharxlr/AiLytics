package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.model.ProcessingQueue;
import com.ailytics.ailytics.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "Endpoints for triggering and tracking AI-powered automation workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "Process document and trigger automation", 
               description = "Uploads a document, extracts data using Gemini 2.0 Flash, and executes a semantic Playwright automation flow.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Workflow enqueued successfully", 
                     content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"jobId\": \"uuid\", \"status\": \"QUEUED\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> processDocument(
            @Parameter(description = "The PDF or Image document to process", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Name of the predefined action (e.g., MEDISEP)", required = true)
            @RequestParam("action") String action,
            @Parameter(description = "Portal login username", required = true)
            @RequestParam("username") String username,
            @Parameter(description = "Portal login password", required = true)
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

    @Operation(summary = "Get workflow job status", 
               description = "Retrieves the current status and result of a previously enqueued automation job.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job status found", 
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcessingQueue.class))),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ProcessingQueue> getStatus(
            @Parameter(description = "The unique ID of the job", required = true)
            @PathVariable String jobId) {
        ProcessingQueue job = workflowService.getJobStatus(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    @Operation(summary = "Get all jobs", description = "Retrieves a history of all automation jobs.")
    @GetMapping("/jobs")
    public ResponseEntity<java.util.List<ProcessingQueue>> getAllJobs() {
        return ResponseEntity.ok(workflowService.getAllJobs());
    }

    @Operation(summary = "Verify and resume a job", 
               description = "Accepts corrected extraction data for a job in AWAITING_APPROVAL state and resumes the automation.")
    @PostMapping("/jobs/{jobId}/verify")
    public ResponseEntity<Map<String, String>> verifyAndResume(
            @PathVariable String jobId, 
            @RequestBody Map<String, Object> correctedData) {
        try {
            workflowService.approveAndResume(jobId, correctedData);
            return ResponseEntity.ok(Map.of("message", "Data verified, resuming automation..."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
