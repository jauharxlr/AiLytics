package com.ailytics.ailytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionConfig {
    @Id
    private String actionName;
    
    @Column(length = 4000)
    private String extractionSchema; // Generic JSON schema for Gemini
    
    private String loginUrl;
    private String usernameSelector;
    private String passwordSelector;
    private String loginSubmitSelector;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<AutomationStep> steps; // The "Recipe" for the wizard flow
}
