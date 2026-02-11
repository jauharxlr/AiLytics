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
public class ActionConfig {
    private String actionName;
    private String portalUrl;
    private String extractionSchema; // String representation of the target JSON schema
    private Map<String, String> formSelectors; // Map field names to CSS selectors
    private String loginUrl;
    private String usernameSelector;
    private String passwordSelector;
    private String submitSelector;
    private String resultSelector; // Selector to capture the Unique ID
}
