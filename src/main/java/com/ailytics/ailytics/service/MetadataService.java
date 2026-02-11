package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetadataService {

    private final Map<String, ActionConfig> configs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Mock MEDISEP Configuration
        ActionConfig medisep = ActionConfig.builder()
                .actionName("MEDISEP")
                .portalUrl("https://medisep.example.com/portal")
                .loginUrl("https://medisep.example.com/login")
                .extractionSchema("{ \"patientName\": \"string\", \"policyNumber\": \"string\", \"amount\": \"number\", \"dateOfService\": \"string\" }")
                .formSelectors(Map.of(
                        "patientName", "#patient-name-input",
                        "policyNumber", "#policy-id",
                        "amount", "input[name='claim-amount']",
                        "dateOfService", "#dos-picker"
                ))
                .usernameSelector("#uid")
                .passwordSelector("#pwd")
                .submitSelector(".btn-login")
                .resultSelector(".success-message .claim-id")
                .build();

        configs.put("MEDISEP", medisep);
    }

    public ActionConfig getConfig(String actionName) {
        return configs.get(actionName);
    }

    public void saveConfig(ActionConfig config) {
        configs.put(config.getActionName(), config);
    }
}
