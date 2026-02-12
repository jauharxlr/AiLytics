package com.ailytics.ailytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
        CAPTURE_RESULT,
        CAPTURE_TO_CONTEXT,
        NEXT_RECIPE
    }

    private StepType type;
    private String targetUrl;       // For NAVIGATE
    private String selector;        // For CLICK, CAPTURE_RESULT, UPLOAD_FILE, CAPTURE_TO_CONTEXT
    private List<String> fields;    // For FILL_FORM: List of JSON keys to fill semantically
    private String contextKey;      // For CAPTURE_TO_CONTEXT: Key to save the value under
    private String nextRecipeName;  // For NEXT_RECIPE: The name of the next action to trigger
    private String waitSelector;    // For WAIT_FOR_LOAD
}
