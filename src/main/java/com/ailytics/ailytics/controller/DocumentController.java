package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ailytics")
@RequiredArgsConstructor
public class DocumentController {

    private final GeminiService geminiService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt) {

        try {
            Resource resource = file.getResource();
            String contentType = file.getContentType();
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Map<String, Object> result = geminiService.processDocumentToStructuredJson(resource, contentType, prompt);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
