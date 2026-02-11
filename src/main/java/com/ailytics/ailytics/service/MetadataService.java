package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.ActionConfig;
import com.ailytics.ailytics.repository.ActionConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final ActionConfigRepository repository;

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            // Seed MEDISEP Configuration
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

            repository.save(medisep);
        }
    }

    public ActionConfig getConfig(String actionName) {
        return repository.findById(actionName).orElse(null);
    }

    public void saveConfig(ActionConfig config) {
        repository.save(config);
    }
}
