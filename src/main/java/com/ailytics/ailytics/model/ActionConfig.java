package com.ailytics.ailytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionConfig {
    @Id
    private String actionName;
    private String portalUrl;
    @Column(length = 2000)
    private String extractionSchema;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "form_selectors", joinColumns = @JoinColumn(name = "action_name"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "selector")
    private Map<String, String> formSelectors;
    
    private String loginUrl;
    private String usernameSelector;
    private String passwordSelector;
    private String submitSelector;
    private String resultSelector;
}
