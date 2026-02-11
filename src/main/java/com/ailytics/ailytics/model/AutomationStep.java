package com.ailytics.ailytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationStep {
    public enum StepType {
        NAVIGATE,
        FILL_FORM,
        CLICK,
        UPLOAD_FILE,
        WAIT_FOR_LOAD,
        CAPTURE_RESULT
    }

    private StepType type;
    private String targetUrl;       // For NAVIGATE
    private String selector;        // For CLICK, CAPTURE_RESULT, UPLOAD_FILE (can be semantic/label)
    private List<String> fields;    // For FILL_FORM: List of JSON keys to fill semantically
    private String waitSelector;    // For WAIT_FOR_LOAD
}
